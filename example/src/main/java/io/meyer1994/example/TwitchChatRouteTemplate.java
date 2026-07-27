package io.meyer1994.example;

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
                                .from("twitch:{{channel}}?event=CHAT")
                                .log("Twitch chat event: ${header['x-camel-twitch-message-id']}")
                                .setHeader("twitchChatEvent").body()
                                .to("sql:" + insertSql + "?noop=true")
                                .setBody(header("twitchChatEvent"))
                                .bean(TwitchStreamService.class, "publish");

                from("sql:SELECT channel_name, count(*) FROM twitch_chat_messages GROUP BY channel_name ORDER BY count(*) DESC LIMIT 10")
                                .log("Channel: ${body}");

                from("direct:emojiCounts")
                                .to("sql:SELECT message FROM twitch_chat_messages WHERE channel_name = :#channel")
                                .bean(EmojiCounter.class, "count");

                from("direct:messageStats")
                                .to("sql:" + """
                                                SELECT DATE_TRUNC('MINUTE', created_at) AS bucket,
                                                       COUNT(*) AS amount
                                                FROM twitch_chat_messages
                                                WHERE channel_name = :#channel
                                                  AND created_at >= DATEADD('HOUR', -1, CURRENT_TIMESTAMP())
                                                GROUP BY DATE_TRUNC('MINUTE', created_at)
                                                ORDER BY bucket
                                                """);

                from("direct:chatterStats")
                                .to("sql:" + """
                                                SELECT DATE_TRUNC('MINUTE', created_at) AS bucket,
                                                       COUNT(DISTINCT user_name) AS amount
                                                FROM twitch_chat_messages
                                                WHERE channel_name = :#channel
                                                  AND created_at >= DATEADD('HOUR', -1, CURRENT_TIMESTAMP())
                                                GROUP BY DATE_TRUNC('MINUTE', created_at)
                                                ORDER BY bucket
                                                """);
        }
}
