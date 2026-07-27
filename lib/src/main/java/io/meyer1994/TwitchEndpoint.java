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

import com.github.twitch4j.chat.ITwitchChat;

/**
 * Receive chat messages from a Twitch channel.
 */
@UriEndpoint(firstVersion = "0.0.2", scheme = "twitch", title = "Twitch", syntax = "twitch:channel", category = Category.SOCIAL, consumerOnly = true, headersClass = TwitchConstants.class)
public class TwitchEndpoint extends DefaultEndpoint {

    @UriParam
    @Metadata(autowired = true)
    private ITwitchChat client;

    @UriPath(description = "Twitch channel login")
    @Metadata(required = true)
    private String channel;

    public TwitchEndpoint() {
    }

    public TwitchEndpoint(String endpointUri, TwitchComponent component) {
        super(endpointUri, component);
    }

    @Override
    public Producer createProducer() {
        throw new UnsupportedOperationException("The Twitch component is consumer-only");
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        TwitchConsumer consumer = new TwitchConsumer(this, processor);
        this.configureConsumer(consumer);
        return consumer;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel.trim().toLowerCase(Locale.ROOT);
    }

    public ITwitchChat getClient() {
        return client;
    }

    /**
     * The externally managed Twitch4J chat client.
     */
    public void setClient(ITwitchChat client) {
        this.client = client;
    }
}
