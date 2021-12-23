package io.meyer1994;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.camel.Exchange;

public class DiscordHandler extends ListenerAdapter {
    private DiscordConsumer consumer;

    public DiscordHandler(DiscordConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Exchange exchange = this.consumer.getEndpoint()
                .createExchange();

        exchange.getMessage()
                .setBody(event.getMessage());
        exchange.getMessage()
                .setHeader(DiscordConstants.CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage()
                .setHeader(DiscordConstants.MESSAGE_ID, event.getMessage().getId());

        try {
            this.consumer.getProcessor()
                    .process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
    }
}