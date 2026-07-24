package io.meyer1994.example;

import org.apache.camel.builder.RouteBuilder;

public class Route extends RouteBuilder {
        @Override
        public void configure() throws Exception {
                // message events
                from("discord:messages?event=onMessageReceived")
                                .log("Message received: ${body.message.contentRaw}")
                                .choice()
                                .when().simple("${body.message.contentRaw} == '!ping'")
                                .log("Responding to !ping")
                                .transform().constant("Pong!")
                                .to("discord:messages?operation=sendMessage")
                                .when().simple("${body.message.contentRaw} == '!nice'")
                                .log("Reacting to !nice")
                                .split()
                                .constant("🇳,🇮,🇨,🇪")
                                .delimiter(",")
                                .to("discord:messages?operation=addReactionById")
                                .end()
                                .end();
                // store message events in database
                from("discord:message-events?event=onMessageReceived")
                                .log("Message received: ${body.message.contentRaw}")
                                .to("sql:insert into event(data) values (:#${body.message.contentRaw})");
                // read message events from database
                from("sql:select * from event order by created_at desc limit 4?delay=10000")
                                .log("Event: ${body}")
                                .transform().simple("log.event: ${body}");

                // reaction events
                from("discord:message-updates?event=onMessageUpdate")
                                .log("Message updated: ${body.message.contentRaw}")
                                .transform().simple("log.onMessageUpdate: ${body.message}")
                                .to("discord:messages?operation=sendMessage");
                from("discord:message-deletions?event=onMessageDelete")
                                .log("Message deleted: ${body.messageId}")
                                .transform().simple("log.onMessageDelete: ${body.messageId}")
                                .to("discord:messages?operation=sendMessage");
                from("discord:message-bulk-deletions?event=onMessageBulkDelete")
                                .log("Messages bulk deleted: ${body.messageIds}")
                                .transform().simple("log.onMessageBulkDelete: ${body.messageIds}")
                                .to("discord:messages?operation=sendMessage");
                from("discord:reaction-additions?event=onMessageReactionAdd")
                                .log("Reaction added: ${body.reaction}")
                                .transform().simple("log.onMessageReactionAdd: emoji=${body.reaction.emoji}")
                                .to("discord:messages?operation=sendMessage");
                from("discord:reaction-removals?event=onMessageReactionRemove")
                                .log("Reaction removed: ${body.reaction}")
                                .transform().simple("log.onMessageReactionRemove: emoji=${body.reaction.emoji}")
                                .to("discord:messages?operation=sendMessage");
                from("discord:reaction-remove-all?event=onMessageReactionRemoveAll")
                                .log("All reactions removed from message: ${body.messageId}")
                                .transform().simple("log.onMessageReactionRemoveAll: messageId=${body.messageId}")
                                .to("discord:messages?operation=sendMessage");
                from("discord:reaction-remove-emoji?event=onMessageReactionRemoveEmoji")
                                .log("Reaction emoji removed: ${body.emoji}")
                                .transform().simple("log.onMessageReactionRemoveEmoji: emoji=${body.emoji}")
                                .to("discord:messages?operation=sendMessage");
        }
}
