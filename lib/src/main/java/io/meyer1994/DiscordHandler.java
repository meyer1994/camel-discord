package io.meyer1994;

import org.apache.camel.Exchange;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEmojiEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/** Bridges JDA message events into Camel exchanges. */
public class DiscordHandler extends ListenerAdapter {
    private final DiscordConsumer consumer;

    public DiscordHandler(DiscordConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!this.consumer.endpoint.name.equals(DiscordConsumer.Names.onMessageReceived.name())) {
            return;
        }

        Message message = event.getMessage();
        Exchange exchange = this.consumer.getEndpoint().createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT, DiscordConsumer.Names.onMessageReceived.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_ID, event.getMessageId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_ID, event.getAuthor().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_IS_BOT, event.getAuthor().isBot());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, event.isFromGuild());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.isFromThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_WEBHOOK, event.isWebhookMessage());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, event.getJumpUrl());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_TIMESTAMP, message.getTimeCreated());
        if (event.isFromGuild()) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        }
        exchange.getMessage().setBody(event);
        this.process(exchange);
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!this.consumer.endpoint.name.equals(DiscordConsumer.Names.onMessageUpdate.name())) {
            return;
        }

        Message message = event.getMessage();
        Exchange exchange = this.consumer.endpoint.createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT, DiscordConsumer.Names.onMessageUpdate.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_ID, event.getMessageId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_ID, event.getAuthor().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_IS_BOT, event.getAuthor().isBot());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, event.isFromGuild());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.isFromThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, event.getJumpUrl());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_TIMESTAMP, message.getTimeCreated());
        if (event.isFromGuild()) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        }
        exchange.getMessage().setBody(event);
        this.process(exchange);
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!this.consumer.endpoint.name.equals(DiscordConsumer.Names.onMessageDelete.name())) {
            return;
        }

        Exchange exchange = this.consumer.endpoint.createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT, DiscordConsumer.Names.onMessageDelete.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_ID, event.getMessageId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, event.isFromGuild());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.isFromThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, event.getJumpUrl());
        if (event.isFromGuild()) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        }
        exchange.getMessage().setBody(event);
        this.process(exchange);
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        if (!this.consumer.endpoint.name.equals(DiscordConsumer.Names.onMessageBulkDelete.name())) {
            return;
        }

        Exchange exchange = this.consumer.endpoint.createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT,
                DiscordConsumer.Names.onMessageBulkDelete.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannel().getType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, true);
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD,
                event.getChannel().getType().isThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_IDS, event.getMessageIds());
        exchange.getMessage().setBody(event);
        this.process(exchange);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!this.consumer.endpoint.name.equals(DiscordConsumer.Names.onMessageReactionAdd.name())) {
            return;
        }

        Exchange exchange = this.consumer.endpoint.createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT,
                DiscordConsumer.Names.onMessageReactionAdd.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_ID, event.getMessageId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, event.isFromGuild());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.isFromThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, event.getJumpUrl());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_REACTION_USER_ID, event.getUserId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_AUTHOR_ID, event.getMessageAuthorId());
        if (event.isFromGuild()) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        }
        exchange.getMessage().setBody(event);
        this.process(exchange);
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        if (!this.consumer.endpoint.name.equals(DiscordConsumer.Names.onMessageReactionRemove.name())) {
            return;
        }

        Exchange exchange = this.consumer.endpoint.createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT,
                DiscordConsumer.Names.onMessageReactionRemove.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_ID, event.getMessageId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, event.isFromGuild());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.isFromThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, event.getJumpUrl());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_REACTION_USER_ID, event.getUserId());
        if (event.isFromGuild()) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        }
        exchange.getMessage().setBody(event);
        this.process(exchange);
    }

    @Override
    public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
        if (!this.consumer.endpoint.name.equals(DiscordConsumer.Names.onMessageReactionRemoveAll.name())) {
            return;
        }

        Exchange exchange = this.consumer.endpoint.createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT,
                DiscordConsumer.Names.onMessageReactionRemoveAll.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_ID, event.getMessageId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, event.isFromGuild());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.isFromThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, event.getJumpUrl());
        if (event.isFromGuild()) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        }
        exchange.getMessage().setBody(event);
        this.process(exchange);
    }

    @Override
    public void onMessageReactionRemoveEmoji(MessageReactionRemoveEmojiEvent event) {
        if (!this.consumer.endpoint.name.equals(DiscordConsumer.Names.onMessageReactionRemoveEmoji.name())) {
            return;
        }

        Exchange exchange = this.consumer.endpoint.createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT,
                DiscordConsumer.Names.onMessageReactionRemoveEmoji.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_ID, event.getMessageId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, event.isFromGuild());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.isFromThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, event.getJumpUrl());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_REACTION_EMOJI, event.getEmoji().getName());
        if (event.isFromGuild()) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        }
        exchange.getMessage().setBody(event);
        this.process(exchange);
    }

    private void process(Exchange exchange) {
        try {
            this.consumer.getProcessor().process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
    }
}
