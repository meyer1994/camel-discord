package io.meyer1994.example;

import org.apache.camel.builder.RouteBuilder;

public class Route extends RouteBuilder {
        @Override
        public void configure() throws Exception {
                // message ping
                from("discord:messages-ping?event=onMessageReceived")
                                .filter().simple("${body.message.contentRaw} == '!ping'")
                                .log("Responding to !ping")
                                .transform().constant("Pong!")
                                .to("seda:ping");
                from("seda:ping?concurrentConsumers=2")
                                .to("discord:messages?operation=sendMessage");

                // message nice
                from("discord:messages?event=onMessageReceived")
                                .filter().simple("${body.message.contentRaw} == '!nice'")
                                .log("Reacting to !nice")
                                .to("seda:nice");
                from("seda:nice?concurrentConsumers=8")
                                .split()
                                .constant("🇳,🇮,🇨,🇪")
                                .delimiter(",")
                                .to("discord:messages?operation=addReactionById")
                                .end();

                // store message events in database
                from("discord:message-events?event=onMessageReceived")
                                .log("Message received: ${body.message.contentRaw}")
                                .to("sql:insert into event(data) values (:#${body.message.contentRaw})");
                // read message events from database
                from("sql:select * from event order by created_at desc limit 4?delay=10000")
                                .log("Event: ${body}")
                                .transform().simple("log.event: ${body}");
        }
}
