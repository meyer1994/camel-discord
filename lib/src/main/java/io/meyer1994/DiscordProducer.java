package io.meyer1994;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public class DiscordProducer extends DefaultProducer {
    private DiscordEndpoint endpoint;

    public static enum Names {
        sendMessage,
        editMessageById,
        deleteMessageById,
        deleteMessagesByIds,
        retrieveMessageById,
        addReactionById,
        removeReactionById,
        clearReactions,
        pin,
        unpin,
        sendTyping,
        reply
    }

    public DiscordProducer(DiscordEndpoint endpoint) {
        super(endpoint);

        if (Names.valueOf(endpoint.getName()) == null) {
            throw new IllegalArgumentException("Unsupported Discord producer method: " + endpoint.getName());
        }

        this.endpoint = endpoint;
    }

    @Override
    public DiscordEndpoint getEndpoint() {
        return this.endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        switch (Names.valueOf(this.endpoint.name)) {
            case sendMessage:
                this.messageSend(exchange);
                return;
            case editMessageById:
                this.messageEdit(exchange);
                return;
            case deleteMessageById:
                this.messageDelete(exchange);
                return;
            case deleteMessagesByIds:
                this.messageBulkDelete(exchange);
                return;
            case retrieveMessageById:
                this.messageRetrieve(exchange);
                return;
            case addReactionById:
                this.messageReact(exchange);
                return;
            case removeReactionById:
                this.messageReactRemove(exchange);
                return;
            case clearReactions:
                this.messageClearReactions(exchange);
                return;
            case pin:
                this.messagePin(exchange);
                return;
            case unpin:
                this.messageUnpin(exchange);
                return;
            case sendTyping:
                this.messageTyping(exchange);
                return;
            case reply:
                this.messageReply(exchange);
                return;
            default:
                throw new IllegalArgumentException("Unsupported Discord operation: " + this.endpoint.name);
        }
    }

    private MessageChannel getChannel(final Exchange exchange) {
        String channelId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_CHANNEL_ID, String.class);
        return this.getEndpoint()
                .getClient()
                .getChannelById(MessageChannel.class, channelId);
    }

    private void messageReply(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        Message target = this.getChannel(exchange)
                .retrieveMessageById(messageId)
                .complete();
        Object body = exchange.getIn().getBody();
        if (body instanceof MessageCreateData createData) {
            exchange.getMessage().setBody(target.reply(createData).complete());
        } else {
            exchange.getMessage().setBody(target.reply((String) body).complete());
        }
    }

    private void messageSend(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body instanceof MessageCreateData createData) {
            exchange.getMessage().setBody(this.getChannel(exchange).sendMessage(createData).complete());
        } else {
            exchange.getMessage().setBody(this.getChannel(exchange).sendMessage((String) body).complete());
        }
    }

    private void messageEdit(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        Object body = exchange.getIn().getBody();
        if (body instanceof MessageEditData editData) {
            exchange.getMessage().setBody(this.getChannel(exchange).editMessageById(messageId, editData).complete());
        } else {
            exchange.getMessage()
                    .setBody(this.getChannel(exchange).editMessageById(messageId, (String) body).complete());
        }
    }

    private void messageDelete(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        this.getChannel(exchange).deleteMessageById(messageId).complete();
    }

    private void messageBulkDelete(Exchange exchange) {
        MessageChannel channel = this.getChannel(exchange);
        if (!(channel instanceof GuildMessageChannel guildChannel)) {
            throw new IllegalStateException("Bulk message deletion requires a guild message channel");
        }
        java.util.Collection<?> ids = exchange.getIn().getHeader(DiscordConstants.HEADER_MESSAGE_IDS,
                java.util.Collection.class);
        java.util.List<String> messageIds = ids.stream()
                .map(String::valueOf)
                .toList();
        guildChannel.deleteMessagesByIds(messageIds).complete();
    }

    private void messageRetrieve(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        exchange.getMessage().setBody(this.getChannel(exchange)
                .retrieveMessageById(messageId)
                .complete());
    }

    private void messageReact(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        Object body = exchange.getIn().getBody();
        Emoji emoji = body instanceof Emoji
                ? (Emoji) body
                : Emoji.fromFormatted((String) body);
        this.getChannel(exchange)
                .addReactionById(messageId, emoji)
                .complete();
    }

    private void messageReactRemove(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        Object body = exchange.getIn().getBody();
        Emoji emoji = body instanceof Emoji
                ? (Emoji) body
                : Emoji.fromFormatted((String) body);
        this.getChannel(exchange)
                .removeReactionById(messageId, emoji)
                .complete();
    }

    private void messageClearReactions(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        this.getChannel(exchange)
                .retrieveMessageById(messageId)
                .complete()
                .clearReactions()
                .complete();
    }

    private void messagePin(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        this.getChannel(exchange)
                .retrieveMessageById(messageId)
                .complete()
                .pin()
                .complete();
    }

    private void messageUnpin(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        this.getChannel(exchange)
                .retrieveMessageById(messageId)
                .complete()
                .unpin()
                .complete();
    }

    private void messageTyping(Exchange exchange) {
        this.getChannel(exchange).sendTyping().complete();
    }

}
