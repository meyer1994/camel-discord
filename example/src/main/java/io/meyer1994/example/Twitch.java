package io.meyer1994.example;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

import reactor.core.publisher.Flux;

@Service
public class Twitch {
    private static final Duration SSE_HEARTBEAT = Duration.ofSeconds(15);

    private static final String MESSAGE_TEMPLATE = """
            <div class="chat chat-start py-0.5" title="%s">
              <div class="chat-header gap-1 text-xs leading-tight">
                <a class="font-semibold hover:text-primary" href="/chatters?chatter=%s">%s</a>
                <time class="opacity-50" title="%s">%s</time>
              </div>
              <div class="chat-bubble min-h-0 px-3 py-1.5 text-sm leading-tight">%s</div>
            </div>
            """.strip();

    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private static final String CHATTER_MESSAGE_TEMPLATE = """
            <div class="chat chat-start py-0.5" title="%s">
              <div class="chat-header gap-1 text-xs leading-tight">
                <span class="font-semibold text-primary">#%s</span>
                <time class="opacity-50" title="%s">%s</time>
              </div>
              <div class="chat-bubble min-h-0 px-3 py-1.5 text-sm leading-tight">%s</div>
            </div>
            """.strip();

    private Topics<ServerSentEvent<String>> topics = new Topics<>();
    private Topics<ServerSentEvent<String>> chatterTopics = new Topics<>();

    public Flux<ServerSentEvent<String>> stream(String channel) {
        return withHeartbeat(topics.subscribe(normalize(channel)));
    }

    public Flux<ServerSentEvent<String>> streamChatter(String chatter) {
        return withHeartbeat(chatterTopics.subscribe(normalize(chatter)));
    }

    public void publish(ChannelMessageEvent event) {
        String channel = normalize(event.getChannel().getName());
        String chatter = normalize(event.getUser().getName());
        String chatterUrl = URLEncoder.encode(event.getUser().getName(), StandardCharsets.UTF_8);

        String html = MESSAGE_TEMPLATE.formatted(
                event.getFiredAtInstant(),
                chatterUrl,
                event.getUser().getName(),
                event.getFiredAtInstant(),
                CHAT_TIME_FORMAT.format(event.getFiredAtInstant()),
                event.getMessage());

        String chatterHtml = CHATTER_MESSAGE_TEMPLATE.formatted(
                event.getFiredAtInstant(),
                event.getChannel().getName(),
                event.getFiredAtInstant(),
                CHAT_TIME_FORMAT.format(event.getFiredAtInstant()),
                event.getMessage());

        ServerSentEvent.Builder<String> builder = ServerSentEvent.builder(html);
        builder.event("chat");
        builder.id(event.getMessageEvent().getMessageId().orElse("-1"));

        ServerSentEvent.Builder<String> chatterBuilder = ServerSentEvent.builder(chatterHtml);
        chatterBuilder.event("chat");
        chatterBuilder.id(event.getMessageEvent().getMessageId().orElse("-1"));

        topics.publish(channel, builder.build());
        chatterTopics.publish(chatter, chatterBuilder.build());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static Flux<ServerSentEvent<String>> withHeartbeat(Flux<ServerSentEvent<String>> events) {
        Flux<ServerSentEvent<String>> heartbeats = Flux.interval(SSE_HEARTBEAT)
                .map(tick -> ServerSentEvent.<String>builder().comment("keepalive").build());
        return Flux.merge(events, heartbeats);
    }
}
