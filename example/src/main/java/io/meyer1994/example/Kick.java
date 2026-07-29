package io.meyer1994.example;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.camel.Exchange;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import io.meyer1994.KickChatMessage;
import io.meyer1994.KickConstants;
import reactor.core.publisher.Flux;

@Service
public class Kick {
    private static final String MESSAGE_TEMPLATE = """
            <div class="chat chat-start py-0.5" title="%s">
              <div class="chat-header gap-1 text-xs leading-tight">
                <span class="badge badge-info badge-xs">Kick</span>
                <span class="font-semibold">%s</span>
                <time class="opacity-50" title="%s">%s</time>
              </div>
              <div class="chat-bubble min-h-0 px-3 py-1.5 text-sm leading-tight">%s</div>
            </div>
            """.strip();

    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Topics<ServerSentEvent<String>> topics = new Topics<>();

    public Flux<ServerSentEvent<String>> stream(String channel) {
        return topics.subscribe(channel);
    }

    public void publish(Exchange exchange) {
        KickChatMessage message = exchange.getMessage().getBody(KickChatMessage.class);
        String channel = exchange.getMessage().getHeader(
                KickConstants.HEADER_CHANNEL_NAME, String.class);
        Instant timestamp = timestamp(message.createdAt());
        String timestampText = CHAT_TIME_FORMAT.format(timestamp);
        String timestampTitle = HtmlUtils.htmlEscape(timestamp.toString());
        String username = HtmlUtils.htmlEscape(message.sender().username());
        String html = MESSAGE_TEMPLATE.formatted(
                timestampTitle,
                username,
                timestampTitle,
                timestampText,
                HtmlUtils.htmlEscape(message.content()));

        ServerSentEvent<String> event = ServerSentEvent.<String>builder(html)
                .event("chat")
                .id("kick:" + message.id())
                .build();
        topics.publish(channel, event);
    }

    private static Instant timestamp(String createdAt) {
        if (createdAt != null && !createdAt.isBlank()) {
            try {
                return Instant.parse(createdAt);
            } catch (RuntimeException ignored) {
                // Render malformed upstream timestamps with a useful fallback.
            }
        }
        return Instant.now();
    }
}
