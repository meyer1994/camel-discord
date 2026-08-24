package io.meyer1994.example;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.camel.Exchange;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

import reactor.core.publisher.Flux;

@Service
public class Twitch {
    private static final String MESSAGE_TEMPLATE = """
            <div id="%s" class="chat chat-start py-1" title="%s">
              <div class="chat-header gap-1 text-xs leading-tight">
                <span class="badge badge-outline badge-xs rounded-none border-slate-600 text-slate-400">Twitch</span>
                <span class="font-semibold">%s</span>
                <span id="%s" class="badge badge-outline badge-xs rounded-none border-slate-600 bg-slate-800/40 text-slate-300">score: …</span>
                <time class="opacity-50" title="%s">%s</time>
              </div>
              <div class="chat-bubble min-h-0 rounded-none border border-slate-700/80 bg-[#0d131d] px-3 py-1.5 text-sm leading-tight">%s</div>
            </div>
            """.strip();

    private static final String SCORE_TEMPLATE = """
            <span id="%s" hx-swap-oob="outerHTML" class="badge badge-outline badge-xs rounded-none border-slate-600 bg-slate-800/40 text-slate-300">score: %s</span>
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
        String messageId = event.getMessageEvent().getMessageId().orElse("-1");

        String html = MESSAGE_TEMPLATE.formatted(
                messageElementId(messageId),
                firedAt,
                escapedChatter,
                scoreElementId(messageId),
                firedAt,
                CHAT_TIME_FORMAT.format(event.getFiredAtInstant()),
                message);

        ServerSentEvent.Builder<String> builder = ServerSentEvent.builder(html);
        builder.event("chat");
        builder.id("twitch:" + event.getMessageEvent().getMessageId().orElse("-1"));

        topics.publish(channel, builder.build());
    }

    public void publishScore(Exchange exchange) {
        String channel = exchange.getVariable("channel_name", String.class);
        String messageId = exchange.getVariable("message_id", String.class);
        Integer score = exchange.getVariable("sentimentScore", Integer.class);
        if (channel == null || messageId == null || score == null) {
            return;
        }

        String html = SCORE_TEMPLATE.formatted(scoreElementId(messageId), score);
        ServerSentEvent<String> event = ServerSentEvent.<String>builder(html)
                .event("chat")
                .id("twitch-score:" + messageId)
                .build();
        topics.publish(channel, event);
    }

    private static String messageElementId(String messageId) {
        return "message-twitch-" + HtmlUtils.htmlEscape(messageId);
    }

    private static String scoreElementId(String messageId) {
        return "score-twitch-" + HtmlUtils.htmlEscape(messageId);
    }

}
