package io.meyer1994.example;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TwitchChatRouteTemplate extends RouteBuilder {
    static final String TEMPLATE_NAME = "twitchChatSse";

    @Override
    public void configure() {
        routeTemplate(TEMPLATE_NAME)
                .templateParameter("channel")
                .from("twitch:{{channel}}")
                .to("bean:chatMessagePublisher?method=publish");
    }
}
