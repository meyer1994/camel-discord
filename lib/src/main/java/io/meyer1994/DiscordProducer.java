package io.meyer1994;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;

public class DiscordProducer extends DefaultProducer {
    private DiscordEndpoint endpoint;

    public DiscordProducer(DiscordEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public DiscordEndpoint getEndpoint() {
        return this.endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        DiscordOperation operation = this.getEndpoint()
                .getOperation();

        switch (operation) {
            case MESSAGE_SEND:
                this.messageSend(exchange);
                return;
            case MESSAGE_EDIT:
                this.messageEdit(exchange);
                return;
            case MESSAGE_DELETE:
                this.messageDelete(exchange);
                return;
            case MESSAGE_BULK_DELETE:
                this.messageBulkDelete(exchange);
                return;
            case MESSAGE_RETRIEVE:
                this.messageRetrieve(exchange);
                return;
            case MESSAGE_REACT:
                this.messageReact(exchange);
                return;
            case MESSAGE_REACT_REMOVE:
                this.messageReactRemove(exchange);
                return;
            case MESSAGE_CLEAR_REACTIONS:
                this.messageClearReactions(exchange);
                return;
            case MESSAGE_PIN:
                this.messagePin(exchange);
                return;
            case MESSAGE_UNPIN:
                this.messageUnpin(exchange);
                return;
            case MESSAGE_TYPING:
                this.messageTyping(exchange);
                return;
            case MESSAGE_REPLY:
                this.messageReply(exchange);
                return;
            default:
                throw new IllegalArgumentException("Unsupported Discord operation: " + operation);
        }
    }

    protected MessageChannel getChannel(final Exchange exchange) {
        String channelId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_CHANNEL_ID, String.class);
        return this.getEndpoint()
                .getClient()
                .getChannelById(MessageChannel.class, channelId);
    }

    protected void messageReply(Exchange exchange) {
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

    protected void messageSend(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body instanceof MessageCreateData createData) {
            exchange.getMessage().setBody(this.getChannel(exchange).sendMessage(createData).complete());
        } else {
            exchange.getMessage().setBody(this.getChannel(exchange).sendMessage((String) body).complete());
        }
    }

    protected void messageEdit(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        Object body = exchange.getIn().getBody();
        if (body instanceof MessageEditData editData) {
            exchange.getMessage().setBody(this.getChannel(exchange).editMessageById(messageId, editData).complete());
        } else {
            exchange.getMessage().setBody(this.getChannel(exchange).editMessageById(messageId, (String) body).complete());
        }
    }

    protected void messageDelete(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        this.getChannel(exchange).deleteMessageById(messageId).complete();
    }

    protected void messageBulkDelete(Exchange exchange) {
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

    protected void messageRetrieve(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        exchange.getMessage().setBody(this.getChannel(exchange)
                .retrieveMessageById(messageId)
                .complete());
    }

    protected void messageReact(Exchange exchange) {
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

    protected void messageReactRemove(Exchange exchange) {
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

    protected void messageClearReactions(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        this.getChannel(exchange)
                .retrieveMessageById(messageId)
                .complete()
                .clearReactions()
                .complete();
    }

    protected void messagePin(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        this.getChannel(exchange)
                .retrieveMessageById(messageId)
                .complete()
                .pin()
                .complete();
    }

    protected void messageUnpin(Exchange exchange) {
        String messageId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
        this.getChannel(exchange)
                .retrieveMessageById(messageId)
                .complete()
                .unpin()
                .complete();
    }

    protected void messageTyping(Exchange exchange) {
        this.getChannel(exchange).sendTyping().complete();
    }

}
