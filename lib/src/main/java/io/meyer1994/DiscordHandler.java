package io.meyer1994;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEmojiEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.camel.Exchange;

/** Bridges JDA message events into Camel exchanges. */
public class DiscordHandler extends ListenerAdapter {
    private final DiscordConsumer consumer;

    public DiscordHandler(DiscordConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        Exchange exchange = createMessageExchange(DiscordEvent.ON_MESSAGE, event, event);
        setAuthorHeaders(exchange, event.getAuthor().getId(), event.getAuthor().isBot());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_WEBHOOK, event.isWebhookMessage());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_TIMESTAMP, message.getTimeCreated());
        processIfSelected(DiscordEvent.ON_MESSAGE, exchange);
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        Message message = event.getMessage();
        Exchange exchange = createMessageExchange(DiscordEvent.ON_MESSAGE_UPDATE, event, event);
        setAuthorHeaders(exchange, event.getAuthor().getId(), event.getAuthor().isBot());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_TIMESTAMP, message.getTimeCreated());
        processIfSelected(DiscordEvent.ON_MESSAGE_UPDATE, exchange);
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        Exchange exchange = createMessageExchange(DiscordEvent.ON_MESSAGE_DELETE, event, event);
        processIfSelected(DiscordEvent.ON_MESSAGE_DELETE, exchange);
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        Exchange exchange = this.consumer.getEndpoint().createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT, DiscordEvent.ON_MESSAGE_BULK_DELETE.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannel().getType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, true);
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.getChannel().getType().isThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_IDS, event.getMessageIds());
        exchange.getMessage().setBody(event);
        processIfSelected(DiscordEvent.ON_MESSAGE_BULK_DELETE, exchange);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        Exchange exchange = createReactionExchange(DiscordEvent.ON_MESSAGE_REACTION_ADD, event);
        exchange.getMessage().setHeader(DiscordConstants.HEADER_REACTION_USER_ID, event.getUserId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_AUTHOR_ID, event.getMessageAuthorId());
        processIfSelected(DiscordEvent.ON_MESSAGE_REACTION_ADD, exchange);
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        Exchange exchange = createReactionExchange(DiscordEvent.ON_MESSAGE_REACTION_REMOVE, event);
        exchange.getMessage().setHeader(DiscordConstants.HEADER_REACTION_USER_ID, event.getUserId());
        processIfSelected(DiscordEvent.ON_MESSAGE_REACTION_REMOVE, exchange);
    }

    @Override
    public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
        Exchange exchange = createMessageExchange(DiscordEvent.ON_MESSAGE_REACTION_REMOVE_ALL, event, event);
        processIfSelected(DiscordEvent.ON_MESSAGE_REACTION_REMOVE_ALL, exchange);
    }

    @Override
    public void onMessageReactionRemoveEmoji(MessageReactionRemoveEmojiEvent event) {
        Exchange exchange = createMessageExchange(DiscordEvent.ON_MESSAGE_REACTION_REMOVE_EMOJI, event, event);
        exchange.getMessage().setHeader(DiscordConstants.HEADER_REACTION_EMOJI, event.getEmoji().getName());
        processIfSelected(DiscordEvent.ON_MESSAGE_REACTION_REMOVE_EMOJI, exchange);
    }

    private Exchange createMessageExchange(DiscordEvent type, GenericMessageEvent event, Object body) {
        Exchange exchange = this.consumer.getEndpoint().createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT, type.name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_ID, event.getMessageId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, event.isFromGuild());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, event.isFromThread());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, event.getJumpUrl());
        if (event.isFromGuild()) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, event.getGuild().getId());
        }
        exchange.getMessage().setBody(body);
        return exchange;
    }

    private Exchange createReactionExchange(DiscordEvent type, GenericMessageReactionEvent event) {
        Exchange exchange = createMessageExchange(type, event, event);
        return exchange;
    }

    private void setAuthorHeaders(Exchange exchange, String authorId, boolean bot) {
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_ID, authorId);
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_IS_BOT, bot);
    }

    private void processIfSelected(DiscordEvent type, Exchange exchange) {
        if (this.consumer.getEndpoint().getEvent() == type) {
            process(exchange);
        }
    }

    private void process(Exchange exchange) {
        try {
            this.consumer.getProcessor().process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
    }
}
