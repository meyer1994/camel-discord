package io.meyer1994.example;

import org.apache.camel.builder.RouteBuilder;

public class Route extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("discord:ping")
                .log("Message received: ${body.message.contentRaw}")
                .filter().simple("${body.message.contentRaw} == '!ping'")
                .log("Responding to !ping")
                .transform().constant("Pong!")
                .to("discord:pong");

        from("discord:nice")
                .log("Message received: ${body.message.contentRaw}")
                .filter().simple("${body.message.contentRaw} == '!nice'")
                .log("Reacting to !nice")
                .split()
                    .constant("🇳,🇮,🇨,🇪")
                    .delimiter(",")
                    .to("discord:nice-reaction?operation=MESSAGE_REACT")
                .end()
                .transform().constant("Nice!")
                .to("discord:nice?operation=MESSAGE_SEND");
        
        from("discord:message-events?event=ON_MESSAGE")
                .log("Message received: ${body.message.contentRaw}");

        from("discord:message-update-events?event=ON_MESSAGE_UPDATE")
                .log("Message updated: ${body.message.contentRaw}");

        from("discord:message-delete-events?event=ON_MESSAGE_DELETE")
                .log("Message deleted: ${body.messageId}");

        from("discord:message-bulk-delete-events?event=ON_MESSAGE_BULK_DELETE")
                .log("Messages bulk deleted: ${body.messageIds}");

        from("discord:reaction-add-events?event=ON_MESSAGE_REACTION_ADD")
                .log("Reaction added: ${body.reaction}");

        from("discord:reaction-remove-events?event=ON_MESSAGE_REACTION_REMOVE")
                .log("Reaction removed: ${body.reaction}");

        from("discord:reaction-remove-all-events?event=ON_MESSAGE_REACTION_REMOVE_ALL")
                .log("All reactions removed from message: ${body.messageId}");

        from("discord:reaction-remove-emoji-events?event=ON_MESSAGE_REACTION_REMOVE_EMOJI")
                .log("Reaction emoji removed: ${body.emoji}");
    }
}
