package io.meyer1994.example;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.http.codec.ServerSentEvent;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

import reactor.core.publisher.FluxSink;

@Component("chatMessagePublisher")
public class ChatMessagePublisher {
    private static final String MESSAGE_TEMPLATE = """
            <li class="px-5 py-4"><span class="font-semibold text-violet-300">%s:</span> %s</li>
            """.strip();

    private final Map<String, FluxSink<ServerSentEvent<String>>> sinks = new ConcurrentHashMap<>();

    public void register(String channel, FluxSink<ServerSentEvent<String>> sink) {
        sinks.put(channel, sink);
    }

    public void unregister(String channel, FluxSink<ServerSentEvent<String>> sink) {
        sinks.remove(channel, sink);
    }

    public void publish(ChannelMessageEvent event) {
        String channel = event.getChannel().getName().toLowerCase(Locale.ROOT);
        FluxSink<ServerSentEvent<String>> sink = sinks.get(channel);
        if (sink == null) {
            return;
        }

        String html = MESSAGE_TEMPLATE.formatted(
                escapeHtml(event.getUser().getName()),
                escapeHtml(event.getMessage()));
        ServerSentEvent.Builder<String> builder = ServerSentEvent.builder(html);
        builder.event("chat");
        event.getMessageEvent().getMessageId().ifPresent(builder::id);
        sink.next(builder.build());
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
