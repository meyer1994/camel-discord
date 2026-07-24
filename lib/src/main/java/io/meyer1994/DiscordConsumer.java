package io.meyer1994;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

public class DiscordConsumer extends DefaultConsumer {
    public static enum Names {
        onMessageReceived,
        onMessageUpdate,
        onMessageDelete,
        onMessageBulkDelete,
        onMessageReactionAdd,
        onMessageReactionRemove,
        onMessageReactionRemoveAll,
        onMessageReactionRemoveEmoji
    }

    protected DiscordEndpoint endpoint;
    protected DiscordHandler handler;

    public DiscordConsumer(DiscordEndpoint endpoint, Processor processor) {
        super(endpoint, processor);

        try {
            Names.valueOf(endpoint.name);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported Discord listener method: " + endpoint.name);
        }

        this.endpoint = endpoint;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        this.handler = new DiscordHandler(this);
        this.endpoint.client.addEventListener(this.handler);
    }

    @Override
    protected void doStop() throws Exception {
        if (this.handler != null) {
            this.endpoint.client.removeEventListener(this.handler);
            this.handler = null;
        }
        super.doStop();
    }

    public DiscordEndpoint getEndpoint() {
        return endpoint;
    }

    public DiscordHandler getHandler() {
        return handler;
    }
}
