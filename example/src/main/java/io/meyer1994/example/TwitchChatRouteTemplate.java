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
                .from("twitch:{{channel}}")
                .setHeader("twitchChatEvent").body()
                .to("sql:" + insertSql + "?noop=true")
                .setBody(header("twitchChatEvent"))
                .to("bean:twitchStreamService?method=publish");

        from("sql:SELECT channel_name, count(*) FROM twitch_chat_messages GROUP BY channel_name ORDER BY count(*) DESC LIMIT 10")
                .split().body()
                .log("Channel: ${body}");
    }
}
