package io.meyer1994.example;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TwitchChatRouteTemplate extends RouteBuilder {
    static final String TEMPLATE_NAME = "twitchChatSse";

    @Override
    public void configure() {
        String insertSql = """
                INSERT INTO twitch_chat_messages (
                    channel_id,
                    channel_name,
                    user_id,
                    user_name,
                    message_id,
                    message,
                    raw_event
                ) VALUES (
                    :#${header['x-camel-twitch-channel-id']},
                    :#${header['x-camel-twitch-channel-name']},
                    :#${header['x-camel-twitch-user-id']},
                    :#${header['x-camel-twitch-user-name']},
                    :#${header['x-camel-twitch-message-id']},
                    :#${body.message},
                    :#${body.messageEvent.rawMessage}
                )
                """;

        routeTemplate(TEMPLATE_NAME)
                .templateParameter("channel")
                .from("twitch:{{channel}}")
                .setHeader("twitchChatEvent").body()
                .to("sql:" + insertSql + "?noop=true")
                .setBody(header("twitchChatEvent"))
                .to("bean:twitchStreamService?method=publish");

        from("sql:SELECT channel_name, count(*) FROM twitch_chat_messages GROUP BY channel_name ORDER BY count(*) DESC LIMIT 10")
                .split().body()
                .log("Channel: ${body}");

        from("direct:emojiCounts")
                .to("sql:SELECT message FROM twitch_chat_messages")
                .process(this::countEmojis);
    }

    private void countEmojis(Exchange exchange) {
        Map<String, Long> counts = ((List<Map<String, Object>>) exchange.getMessage().getBody())
                .stream()
                .map(row -> (String) row.get("MESSAGE"))
                .flatMapToInt(String::codePoints)
                .filter(this::isEmoji)
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .collect(Collectors.groupingBy(
                        emoji -> emoji,
                        LinkedHashMap::new,
                        Collectors.counting()));

        exchange.getMessage().setBody(counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new)));
    }

    private boolean isEmoji(int codePoint) {
        return codePoint >= 0x1F000 && codePoint <= 0x1FAFF
                || codePoint >= 0x2600 && codePoint <= 0x27BF;
    }
}
