package io.meyer1994;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordProducer extends DefaultProducer {
    private static final Logger LOG = LoggerFactory.getLogger(DiscordProducer.class);
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
    public void process(Exchange exchange) {
        DiscordOperation operation = this.getEndpoint()
                .getOperation();

        switch (operation) {
            case MESSAGE_SEND:
                this.messageSend(exchange);
                return;
            case MESSAGE_REACT:
                this.messageReact(exchange);
                return;
            case MESSAGE_REPLY:
                this.messageReply(exchange);
        }
    }

    protected TextChannel getChannel(final Exchange exchange) {
        String channelId = exchange.getIn()
                .getHeader(DiscordConstants.CHANNEL_ID, String.class);
        return this.getEndpoint()
                .getClient()
                .getTextChannelById(channelId);
    }

    protected void messageReply(Exchange exchange) {
        String message = exchange.getIn()
                .getHeader(DiscordConstants.MESSAGE_ID, String.class);
        String reply = exchange.getIn()
                .getBody(String.class);
        this.getChannel(exchange)
                .retrieveMessageById(message)
                .queue(m -> m.reply(reply).queue());
    }

    protected void messageSend(Exchange exchange) {
        String message = exchange.getIn().getBody(String.class);
        this.getChannel(exchange)
                .sendMessage(message)
                .queue();
    }

    protected void messageReact(Exchange exchange) {
        String message = exchange.getIn()
                .getHeader(DiscordConstants.MESSAGE_ID, String.class);
        String emote = exchange.getIn()
                .getBody(String.class);
        this.getChannel(exchange)
                .addReactionById(message, Emoji.fromUnicode(emote))
                .queue();
    }
}
