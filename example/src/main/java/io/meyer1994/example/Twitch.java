package io.meyer1994.example;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Bridges Twitch chat from Camel into per-channel SSE fluxes, and owns Twitch Camel route lifecycle
 * for each channel while listeners are connected.
 */
@Service
public class Twitch {
  private static final Logger logger = LoggerFactory.getLogger(Twitch.class);
  private static final DateTimeFormatter CHAT_TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

  @Autowired private TemplateEngine templateEngine;

  @Autowired private CamelContext camelContext;

  private final Map<String, Sinks.Many<ServerSentEvent<String>>> streams =
      new ConcurrentHashMap<>();
  private final Object routeLock = new Object();

  public Flux<ServerSentEvent<String>> stream(String channel) {
    String key = channel.trim().toLowerCase();
    ensureRoute(key);

    Sinks.Many<ServerSentEvent<String>> sink =
        streams.computeIfAbsent(key, k -> Sinks.many().multicast().directBestEffort());

    return sink.asFlux()
        .doFinally(
            signal -> {
              if (sink.currentSubscriberCount() > 0) {
                return;
              }
              streams.remove(key, sink);
              removeRoute(key);
            });
  }

  public void publish(ChannelMessageEvent event) {
    String channel = event.getChannel().getName();
    String chatter = event.getUser().getName();
    String firedAt = event.getFiredAtInstant().toString();
    String messageId = event.getMessageEvent().getMessageId().orElse("-1");

    Map<String, Object> variables = new HashMap<>();
    variables.put("messageId", messageElementId(messageId));
    variables.put("source", "Twitch");
    variables.put("username", chatter);
    variables.put("title", firedAt);
    variables.put("goodId", goodScoreElementId(messageId));
    variables.put("badId", badScoreElementId(messageId));
    variables.put("goodScore", null);
    variables.put("badScore", null);
    variables.put("timeTitle", firedAt);
    variables.put("timeText", CHAT_TIME_FORMAT.format(event.getFiredAtInstant()));
    variables.put("message", event.getMessage());

    String id = String.format("twitch:%s", messageId);
    String html = render("chat-message", variables);

    var sse = ServerSentEvent.<String>builder(html).id(id).build();
    emit(channel, sse);
  }

  public void publishScore(Exchange exchange) {
    String channel = exchange.getVariable("channel_name", String.class);
    String messageId = exchange.getVariable("message_id", String.class);
    Double good = exchange.getVariable("goodScore", Double.class);
    Double bad = exchange.getVariable("badScore", Double.class);

    if (channel == null || messageId == null || good == null || bad == null) {
      return;
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("goodId", goodScoreElementId(messageId));
    variables.put("goodScore", good);
    variables.put("badId", badScoreElementId(messageId));
    variables.put("badScore", bad);

    String id = String.format("twitch-score:%s", messageId);
    String html = render("chat-score-update", variables);

    var sse = ServerSentEvent.<String>builder(html).id(id).build();
    emit(channel, sse);
  }

  private void emit(String channel, ServerSentEvent<String> sse) {
    if (channel == null) {
      return;
    }
    Sinks.Many<ServerSentEvent<String>> sink = streams.get(channel.toLowerCase());
    if (sink == null) {
      return;
    }
    sink.tryEmitNext(sse);
  }

  private void ensureRoute(String channel) {
    String routeId = routeId(channel);
    synchronized (routeLock) {
      if (camelContext.getRoute(routeId) != null) {
        return;
      }
      try {
        logger.info("Adding TWITCH route for channel: {}", channel);
        camelContext.addRouteFromTemplate(
            routeId, Camel.TWITCH_TEMPLATE_NAME, Map.of("channel", channel));
      } catch (Exception e) {
        throw new IllegalStateException("Failed to add Twitch route for " + channel, e);
      }
    }
  }

  private void removeRoute(String channel) {
    String routeId = routeId(channel);
    synchronized (routeLock) {
      if (streams.containsKey(channel)) {
        return;
      }
      if (camelContext.getRoute(routeId) == null) {
        return;
      }
      try {
        logger.info("Removing TWITCH route for channel: {}", channel);
        camelContext.getRouteController().stopRoute(routeId);
        camelContext.removeRoute(routeId);
      } catch (Exception e) {
        logger.warn("Failed to remove Twitch route for {}: {}", channel, e.toString());
      }
    }
  }

  private static String routeId(String channel) {
    return "twitch:channel:" + channel;
  }

  private String render(String fragment, Map<String, Object> variables) {
    Context context = new Context(Locale.ROOT);
    context.setVariables(variables);
    return templateEngine.process("index", Set.of(fragment), context).strip();
  }

  private static String messageElementId(String messageId) {
    return "message-twitch-" + messageId;
  }

  private static String goodScoreElementId(String messageId) {
    return "good-score-twitch-" + messageId;
  }

  private static String badScoreElementId(String messageId) {
    return "bad-score-twitch-" + messageId;
  }
}
