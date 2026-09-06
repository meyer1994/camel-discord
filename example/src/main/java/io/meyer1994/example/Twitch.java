package io.meyer1994.example;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

import reactor.core.publisher.Flux;

@Service
public class Twitch {
    private static final DateTimeFormatter CHAT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final TemplateEngine templateEngine;
    private final Topics<ServerSentEvent<String>> topics = new Topics<>();

    public Twitch(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public Flux<ServerSentEvent<String>> stream(String channel) {
        return topics.subscribe(channel);
    }

    public void publish(ChannelMessageEvent event) {
        String channel = event.getChannel().getName();
        String chatter = event.getUser().getName();
        String firedAt = event.getFiredAtInstant().toString();
        String messageId = event.getMessageEvent().getMessageId().orElse("-1");

        Map<String, Object> variables = new HashMap<>();
        variables.put("messageId", messageElementId(messageId));
        variables.put("gradientId", gradientElementId(messageId));
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
        variables.put("gradientClasses", initialGradientClasses());

        String html = render("chat-message", variables);

        ServerSentEvent<String> sse = ServerSentEvent.<String>builder(html)
                .id("twitch:" + event.getMessageEvent().getMessageId().orElse("-1"))
                .build();

        topics.publish(channel, sse);
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
        variables.put("gradientId", gradientElementId(messageId));
        variables.put("gradientClasses", scoredGradientClasses(good, bad));

        String html = render("chat-score-update", variables);
        ServerSentEvent<String> sse = ServerSentEvent.<String>builder(html)
                .id("twitch-score:" + messageId)
                .build();
        topics.publish(channel, sse);
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

    private static String gradientElementId(String messageId) {
        return "gradient-twitch-" + messageId;
    }

    private static String initialGradientClasses() {
        return "from-red-500/10 via-transparent via-50% to-green-500/10";
    }

    private static String scoredGradientClasses(double good, double bad) {
        double total = Math.max(0.001, good + bad);
        int badShare = (int) Math.round(bad * 100.0 / total / 10.0) * 10;
        int position = Math.max(0, Math.min(100, badShare));
        return "from-red-500/25 via-transparent via-" + position + "% to-green-500/25";
    }

}
