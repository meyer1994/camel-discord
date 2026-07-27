package io.meyer1994;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.eventsub.events.ChannelCheerEvent;
import com.github.twitch4j.eventsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.eventsub.events.ChannelUpdateV2Event;
import com.github.twitch4j.eventsub.events.EventSubChannelEvent;
import com.github.twitch4j.eventsub.events.EventSubUserChannelEvent;
import com.github.twitch4j.eventsub.events.StreamOfflineEvent;
import com.github.twitch4j.eventsub.events.StreamOnlineEvent;

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

    public void onEvent(Object event) {
        if (event instanceof StreamOnlineEvent streamOnline) {
            processEvent(event, "streamOnline", streamOnline, null, null, streamOnline.getStartedAt());
        } else if (event instanceof StreamOfflineEvent streamOffline) {
            processEvent(event, "streamOffline", streamOffline, null, null, null);
        } else if (event instanceof ChannelUpdateV2Event channelUpdate) {
            processEvent(event, "channelUpdate", channelUpdate, null, null, null);
        } else if (event instanceof ChannelSubscribeEvent subscribe) {
            processEvent(event, "subscribe", subscribe, subscribe.getUserId(), subscribe.getUserName(), null);
        } else if (event instanceof ChannelCheerEvent cheer) {
            processEvent(event, "cheer", cheer, cheer.getUserId(), cheer.getUserName(), null);
        }
    }

    private void processEvent(
            Object event,
            String eventType,
            EventSubChannelEvent channelEvent,
            String userId,
            String userName,
            java.time.Instant timestamp) {
        Exchange exchange = consumer.getEndpoint().createExchange(ExchangePattern.InOnly);
        exchange.getMessage().setBody(event);
        exchange.getMessage().setHeader(TwitchConstants.HEADER_EVENT_TYPE, eventType);
        exchange.getMessage().setHeader(TwitchConstants.HEADER_CHANNEL_ID, channelEvent.getBroadcasterUserId());
        exchange.getMessage().setHeader(TwitchConstants.HEADER_CHANNEL_NAME, channelEvent.getBroadcasterUserLogin());
        if (userId != null) {
            exchange.getMessage().setHeader(TwitchConstants.HEADER_USER_ID, userId);
        }
        if (userName != null) {
            exchange.getMessage().setHeader(TwitchConstants.HEADER_USER_NAME, userName);
        }
        if (timestamp != null) {
            exchange.getMessage().setHeader(TwitchConstants.HEADER_EVENT_TIMESTAMP, timestamp);
        }
        process(exchange, "Error processing Twitch " + eventType + " event");
    }

    private void processEvent(
            Object event,
            String eventType,
            EventSubUserChannelEvent channelEvent,
            String userId,
            String userName,
            java.time.Instant timestamp) {
        Exchange exchange = consumer.getEndpoint().createExchange(ExchangePattern.InOnly);
        exchange.getMessage().setBody(event);
        exchange.getMessage().setHeader(TwitchConstants.HEADER_EVENT_TYPE, eventType);
        exchange.getMessage().setHeader(TwitchConstants.HEADER_CHANNEL_ID, channelEvent.getBroadcasterUserId());
        exchange.getMessage().setHeader(TwitchConstants.HEADER_CHANNEL_NAME, channelEvent.getBroadcasterUserLogin());
        if (userId != null) {
            exchange.getMessage().setHeader(TwitchConstants.HEADER_USER_ID, userId);
        }
        if (userName != null) {
            exchange.getMessage().setHeader(TwitchConstants.HEADER_USER_NAME, userName);
        }
        if (timestamp != null) {
            exchange.getMessage().setHeader(TwitchConstants.HEADER_EVENT_TIMESTAMP, timestamp);
        }
        process(exchange, "Error processing Twitch " + eventType + " event");
    }

    private void process(Exchange exchange, String message) {
        try {
            consumer.getProcessor().process(exchange);
        } catch (Exception e) {
            consumer.getExceptionHandler().handleException(message, exchange, e);
        }
    }
}
