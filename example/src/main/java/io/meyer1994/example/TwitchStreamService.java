package io.meyer1994.example;

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
            <li class="px-5 py-4"><span class="font-semibold text-violet-300">%s:</span> %s</li>
            """.strip();

    private final CamelContext context;
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sinks = new ConcurrentHashMap<>();

    public TwitchStreamService(CamelContext context) {
        this.context = context;
    }

    public Flux<ServerSentEvent<String>> stream(String channel) throws Exception {
        Endpoint endpoint = context.hasEndpoint(String.format("twitch:%s", channel));
        if (endpoint == null) {
            context.addRouteFromTemplate(
                    String.format("twitch:%s", channel),
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

        String html = MESSAGE_TEMPLATE.formatted(
                escapeHtml(event.getUser().getName()),
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
