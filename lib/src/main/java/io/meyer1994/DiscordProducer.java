package io.meyer1994;

import java.util.Collection;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

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
        switch (this.endpoint.getOperation()) {
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
                throw new IllegalArgumentException("Unsupported Discord operation: " + this.endpoint.getOperation());
        }
    }

    private MessageChannel getChannel(final Exchange exchange) {
        String channelId = exchange.getIn()
                .getHeader(DiscordConstants.HEADER_CHANNEL_ID, String.class);
        return this.getEndpoint()
                .getClient()
                .getChannelById(MessageChannel.class, channelId);
    }

    private String getMessageId(final Exchange exchange) {
        return exchange.getIn()
                .getHeader(DiscordConstants.HEADER_MESSAGE_ID, String.class);
    }

    private void messageReply(Exchange exchange) {
        Object body = exchange.getIn().getBody();

        if (body instanceof MessageCreateData data) {
            this.getChannel(exchange)
                    .retrieveMessageById(this.getMessageId(exchange))
                    .queue(msg -> msg.reply(data).queue());
        } else {
            this.getChannel(exchange)
                    .retrieveMessageById(this.getMessageId(exchange))
                    .queue(msg -> msg.reply(body.toString()).queue());
        }
    }

    private void messageSend(Exchange exchange) {
        Object body = exchange.getIn().getBody();

        if (body instanceof MessageCreateData data) {
            this.getChannel(exchange).sendMessage(data).queue();
        } else {
            this.getChannel(exchange).sendMessage(body.toString()).queue();
        }
    }

    private void messageEdit(Exchange exchange) {
        Object body = exchange.getIn().getBody();

        if (body instanceof MessageEditData editData) {
            this.getChannel(exchange)
                    .retrieveMessageById(this.getMessageId(exchange))
                    .queue(msg -> msg.editMessage(editData).queue());
        } else {
            this.getChannel(exchange)
                    .retrieveMessageById(this.getMessageId(exchange))
                    .queue(msg -> msg.editMessage(body.toString()).queue());
        }
    }

    private void messageDelete(Exchange exchange) {
        this.getChannel(exchange)
                .retrieveMessageById(this.getMessageId(exchange))
                .queue(msg -> msg.delete().queue());
    }

    private void messageBulkDelete(Exchange exchange) {
        MessageChannel channel = this.getChannel(exchange);

        if (!(channel instanceof GuildMessageChannel guild)) {
            throw new IllegalStateException("Bulk message deletion requires a guild message channel");
        }

        Collection<?> ids = exchange.getIn().getHeader(DiscordConstants.HEADER_MESSAGE_IDS, Collection.class);
        List<String> messageIds = ids.stream().map(String::valueOf).toList();
        guild.deleteMessagesByIds(messageIds).queue();
    }

    private void messageRetrieve(Exchange exchange) {
        this.getChannel(exchange)
                .retrieveMessageById(this.getMessageId(exchange))
                .submit();
    }

    private void messageReact(Exchange exchange) {
        Object body = exchange.getIn().getBody();

        Emoji emoji = body instanceof Emoji
                ? (Emoji) body
                : Emoji.fromFormatted((String) body);

        this.getChannel(exchange)
                .retrieveMessageById(this.getMessageId(exchange))
                .queue(msg -> msg.addReaction(emoji).queue());
    }

    private void messageReactRemove(Exchange exchange) {
        Object body = exchange.getIn().getBody();

        Emoji emoji = body instanceof Emoji
                ? (Emoji) body
                : Emoji.fromFormatted((String) body);

        this.getChannel(exchange)
                .retrieveMessageById(this.getMessageId(exchange))
                .queue(msg -> msg.removeReaction(emoji).queue());
    }

    private void messageClearReactions(Exchange exchange) {
        this.getChannel(exchange)
                .retrieveMessageById(this.getMessageId(exchange))
                .queue(msg -> msg.clearReactions().queue());
    }

    private void messagePin(Exchange exchange) {
        this.getChannel(exchange)
                .retrieveMessageById(this.getMessageId(exchange))
                .queue(msg -> msg.pin().queue());
    }

    private void messageUnpin(Exchange exchange) {
        this.getChannel(exchange)
                .retrieveMessageById(this.getMessageId(exchange))
                .queue(msg -> msg.unpin().queue());
    }

    private void messageTyping(Exchange exchange) {
        this.getChannel(exchange).sendTyping().queue();
    }

}
