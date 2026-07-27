package io.meyer1994.example;

import org.apache.camel.builder.RouteBuilder;

public class Route extends RouteBuilder {
        @Override
        public void configure() throws Exception {
                from("twitch:cellbit")
                                .log("${body.user.name}: ${body.message}");
        }
}
