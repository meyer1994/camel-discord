package io.meyer1994;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.camel.Exchange;
import net.dv8tion.jda.api.entities.Message;

public class DiscordHandler extends ListenerAdapter {
    private DiscordConsumer consumer;

    public DiscordHandler(DiscordConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Exchange exchange = this.consumer.getEndpoint()
                .createExchange();

        exchange.getMessage()
                .setBody(message);
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_MESSAGE_ID, message.getId());
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_AUTHOR_ID, event.getAuthor().getId());
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_AUTHOR_IS_BOT, event.getAuthor().isBot());
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, event.isFromGuild());
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.isFromThread());
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_IS_WEBHOOK, event.isWebhookMessage());
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_MESSAGE_URL, event.getJumpUrl());
        exchange.getMessage()
                .setHeader(DiscordConstants.HEADER_MESSAGE_TIMESTAMP, message.getTimeCreated());

        if (event.isFromGuild()) {
            exchange.getMessage()
                    .setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        }

        try {
            this.consumer.getProcessor()
                    .process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
    }
}
