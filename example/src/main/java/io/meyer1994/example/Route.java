package io.meyer1994.example;

import org.apache.camel.builder.RouteBuilder;

public class Route extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("discord:onMessageReceived")
                .log("Message received: ${body.message.contentRaw}")
                .choice()
                    .when().simple("${body.message.contentRaw} == '!ping'")
                        .log("Responding to !ping")
                        .transform().constant("Pong!")
                        .to("discord:sendMessage")
                    .when().simple("${body.message.contentRaw} == '!nice'")
                        .log("Reacting to !nice")
                        .split()
                            .constant("🇳,🇮,🇨,🇪")
                            .delimiter(",")
                            .to("discord:addReactionById")
                        .end()
                .end();

        from("discord:onMessageUpdate")
                .log("Message updated: ${body.message.contentRaw}")
                .transform().simple("log.onMessageUpdate: ${body.message}")
                .to("discord:sendMessage");

        from("discord:onMessageDelete")
                .log("Message deleted: ${body.messageId}")
                .transform().simple("log.onMessageDelete: ${body.messageId}")
                .to("discord:sendMessage");

        from("discord:onMessageBulkDelete")
                .log("Messages bulk deleted: ${body.messageIds}")
                .transform().simple("log.onMessageBulkDelete: ${body.messageIds}")
                .to("discord:sendMessage");

        from("discord:onMessageReactionAdd")
                .log("Reaction added: ${body.reaction}")
                .transform().simple("log.onMessageReactionAdd: emoji=${body.reaction.emoji}")
                .to("discord:sendMessage");

        from("discord:onMessageReactionRemove")
                .log("Reaction removed: ${body.reaction}")
                .transform().simple("log.onMessageReactionRemove: emoji=${body.reaction.emoji}")
                .to("discord:sendMessage");

        from("discord:onMessageReactionRemoveAll")
                .log("All reactions removed from message: ${body.messageId}")
                .transform().simple("log.onMessageReactionRemoveAll: messageId=${body.messageId}")
                .to("discord:sendMessage");

        from("discord:onMessageReactionRemoveEmoji")
                .log("Reaction emoji removed: ${body.emoji}")
                .transform().simple("log.onMessageReactionRemoveEmoji: emoji=${body.emoji}")
                .to("discord:sendMessage");
    }
}
