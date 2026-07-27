package io.meyer1994.example;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class TwitchStreamService {
    private static final String MESSAGE_TEMPLATE = """
            <div class="chat chat-start py-0.5" title="%s">
              <div class="chat-header gap-1 text-xs leading-tight">
                %s
                <time class="opacity-50" title="%s">%s</time>
              </div>
              <div class="chat-bubble min-h-0 px-3 py-1.5 text-sm leading-tight">%s</div>
            </div>
            """.strip();

    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final CamelContext context;
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sinks = new ConcurrentHashMap<>();

    public TwitchStreamService(CamelContext context) {
        this.context = context;
    }

    public Flux<ServerSentEvent<String>> stream(String channel) throws Exception {
        Endpoint endpoint = context.hasEndpoint(String.format("twitch:%s?event=CHAT", channel));
        if (endpoint == null) {
            context.addRouteFromTemplate(
                    String.format("twitch:%s?event=CHAT", channel),
                    TwitchChatRouteTemplate.TEMPLATE_NAME,
                    Map.of("channel", channel));
        }

        return sinks
                .computeIfAbsent(channel, ignored -> Sinks.many().multicast().directBestEffort())
                .asFlux();
    }

    public void publish(ChannelMessageEvent event) {
        String channel = event.getChannel().getName().toLowerCase(Locale.ROOT);
        Sinks.Many<ServerSentEvent<String>> sink = sinks.get(channel);

        if (sink == null)
            return;

        Instant createdAt = Instant.now();
        String html = MESSAGE_TEMPLATE.formatted(
                createdAt,
                escapeHtml(event.getUser().getName()),
                createdAt,
                CHAT_TIME_FORMAT.format(createdAt),
                escapeHtml(event.getMessage()));

        ServerSentEvent.Builder<String> builder = ServerSentEvent.builder(html);
        builder.event("chat");
        event.getMessageEvent().getMessageId().ifPresent(builder::id);
        sink.tryEmitNext(builder.build());
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
