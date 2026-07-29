package io.meyer1994;

import java.util.Locale;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/** Receive chat messages from a Kick channel. */
@UriEndpoint(firstVersion = "0.0.2", scheme = "kick", title = "Kick", syntax = "kick:channel",
        category = Category.SOCIAL, consumerOnly = true, headersClass = KickConstants.class)
public class KickEndpoint extends DefaultEndpoint {

    @UriPath(description = "Kick channel slug")
    @Metadata(required = true)
    private String channel;

    @UriParam(defaultValue = "CHAT", description = "Event to consume. Only CHAT is supported")
    private KickEvent event = KickEvent.CHAT;

    @UriParam(description = "Kick chatroom ID. When omitted, the component resolves it from the channel slug")
    private Long chatroomId;

    @UriParam(defaultValue = "5000", description = "Delay in milliseconds before reconnecting")
    private long reconnectDelay = 5_000;

    @UriParam(defaultValue = "10000", description = "HTTP and WebSocket connection timeout in milliseconds")
    private int connectionTimeout = 10_000;

    public KickEndpoint() {
    }

    public KickEndpoint(String endpointUri, KickComponent component) {
        super(endpointUri, component);
    }

    @Override
    public Producer createProducer() {
        throw new UnsupportedOperationException("The Kick component is consumer-only");
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        KickConsumer consumer = new KickConsumer(this, processor);
        configureConsumer(consumer);
        return consumer;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("Kick channel must not be blank");
        }
        this.channel = channel.trim().toLowerCase(Locale.ROOT);
    }

    public KickEvent getEvent() {
        return event;
    }

    public void setEvent(KickEvent event) {
        this.event = event;
    }

    public Long getChatroomId() {
        return chatroomId;
    }

    public void setChatroomId(Long chatroomId) {
        if (chatroomId != null && chatroomId <= 0) {
            throw new IllegalArgumentException("Kick chatroomId must be positive");
        }
        this.chatroomId = chatroomId;
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public void setReconnectDelay(long reconnectDelay) {
        if (reconnectDelay < 0) {
            throw new IllegalArgumentException("Kick reconnectDelay must not be negative");
        }
        this.reconnectDelay = reconnectDelay;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        if (connectionTimeout <= 0) {
            throw new IllegalArgumentException("Kick connectionTimeout must be positive");
        }
        this.connectionTimeout = connectionTimeout;
    }
}
