package io.meyer1994;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

/** Bridges Twitch4J channel messages into Camel exchanges. */
public class TwitchHandler {
    private final TwitchConsumer consumer;

    public TwitchHandler(TwitchConsumer consumer) {
        this.consumer = consumer;
    }

    public void onChannelMessage(ChannelMessageEvent event) {
        if (!consumer.getEndpoint().getChannel().equalsIgnoreCase(event.getChannel().getName())) {
            return;
        }

        Exchange exchange = consumer.getEndpoint().createExchange(ExchangePattern.InOnly);
        exchange.getMessage().setBody(event);
        exchange.getMessage().setHeader(TwitchConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(TwitchConstants.HEADER_CHANNEL_NAME, event.getChannel().getName());
        exchange.getMessage().setHeader(TwitchConstants.HEADER_USER_ID, event.getUser().getId());
        exchange.getMessage().setHeader(TwitchConstants.HEADER_USER_NAME, event.getUser().getName());
        event.getMessageEvent()
                .getMessageId()
                .ifPresent(messageId -> exchange.getMessage().setHeader(TwitchConstants.HEADER_MESSAGE_ID, messageId));

        try {
            consumer.getProcessor().process(exchange);
        } catch (Exception e) {
            consumer.getExceptionHandler()
                    .handleException("Error processing Twitch chat message", exchange, e);
        }
    }
}
