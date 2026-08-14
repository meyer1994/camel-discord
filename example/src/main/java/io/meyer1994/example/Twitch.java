package io.meyer1994.example;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

import reactor.core.publisher.Flux;

@Service
public class Twitch {
    private static final String MESSAGE_TEMPLATE = """
            <div class="chat chat-start py-0.5" title="%s">
              <div class="chat-header gap-1 text-xs leading-tight">
                <span class="badge badge-secondary badge-xs">Twitch</span>
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

    public void publish(ChannelMessageEvent event) {
        String channel = event.getChannel().getName();
        String chatter = event.getUser().getName();
        String escapedChatter = HtmlUtils.htmlEscape(chatter);
        String message = HtmlUtils.htmlEscape(event.getMessage());
        String firedAt = HtmlUtils.htmlEscape(event.getFiredAtInstant().toString());

        String html = MESSAGE_TEMPLATE.formatted(
                firedAt,
                escapedChatter,
                firedAt,
                CHAT_TIME_FORMAT.format(event.getFiredAtInstant()),
                message);

        ServerSentEvent.Builder<String> builder = ServerSentEvent.builder(html);
        builder.event("chat");
        builder.id("twitch:" + event.getMessageEvent().getMessageId().orElse("-1"));

        topics.publish(channel, builder.build());
    }

}
