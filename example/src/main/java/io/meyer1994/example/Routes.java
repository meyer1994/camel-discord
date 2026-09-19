package io.meyer1994.example;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import reactor.core.publisher.Flux;

@Controller
public class Routes {
  private static final Logger logger = LoggerFactory.getLogger(Routes.class);

  @Autowired
  private Twitch twitch;

  @Autowired
  private Kick kick;

  @Autowired
  private CamelContext camelContext;

  @Autowired
  private Chart chart;

  @Value("${app.twitch.channels}")
  private Set<String> channels = Collections.emptySet();

  private final Object lock = new Object();

  @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
  public String index(Model model) {
    model.addAttribute("channels", channels);
    model.addAttribute("feed", false);
    return "index";
  }

  @GetMapping(path = "/c", produces = MediaType.TEXT_HTML_VALUE)
  public String selectChannel(@RequestParam("channel") String channel) {
    if (channel == null || channel.isBlank()) {
      return "redirect:/";
    }
    return String.format("redirect:/c/%s", channel.trim().toLowerCase());
  }

  @GetMapping(path = "/c/{channel}", produces = MediaType.TEXT_HTML_VALUE)
  public String channel(@PathVariable("channel") String channel, Model model) throws Exception {
    synchronized (lock) {
      String kickId = String.format("kick:channel:%s", channel.toLowerCase());
      String twitchId = String.format("twitch:channel:%s", channel.toLowerCase());

      if (camelContext.getRoute(kickId) == null) {
        logger.info("Adding KICK route for channel: {}", channel);
        camelContext.addRouteFromTemplate(
            kickId, Camel.KICK_TEMPLATE_NAME, Map.of("channel", channel));
      }

      if (camelContext.getRoute(twitchId) == null) {
        logger.info("Adding TWITCH route for channel: {}", channel);
        camelContext.addRouteFromTemplate(
            twitchId, Camel.TWITCH_TEMPLATE_NAME, Map.of("channel", channel));
      }
    }

    model.addAttribute("feed", true);
    model.addAttribute("channel", channel);
    return "index";
  }

  @ResponseBody
  @GetMapping(path = "/c/{channel}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> events(@PathVariable("channel") String channel) {
    return withHeartbeat(Flux.merge(kick.stream(channel), twitch.stream(channel)));
  }

  @ResponseBody
  @GetMapping(path = "/chart/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> chartData() {
    return withHeartbeat(chart.stream());
  }

  private static Flux<ServerSentEvent<String>> withHeartbeat(Flux<ServerSentEvent<String>> events) {
    return Flux.merge(
        events,
        Flux.interval(Duration.ofSeconds(15))
            .map(tick -> ServerSentEvent.<String>builder().comment("keepalive").build()));
  }
}
