package io.meyer1994;

import java.util.Set;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

public class DiscordConsumer extends DefaultConsumer {
    private static final Set<String> LISTENER_NAMES = Set.of(
            "onMessageReceived",
            "onMessageUpdate",
            "onMessageDelete",
            "onMessageBulkDelete",
            "onMessageReactionAdd",
            "onMessageReactionRemove",
            "onMessageReactionRemoveAll",
            "onMessageReactionRemoveEmoji"
    );

    private DiscordEndpoint endpoint;
    private DiscordHandler handler;

    public DiscordConsumer(DiscordEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        
        if (!LISTENER_NAMES.contains(endpoint.getName())) {
            throw new IllegalArgumentException("Unsupported Discord listener method: " + endpoint.getName());
        }

        this.endpoint = endpoint;
    }

    @Override
    public DiscordEndpoint getEndpoint() {
        return this.endpoint;
    }

    @Override
    protected void doInit() throws Exception {
        super.doInit();
        this.handler = new DiscordHandler(this);
        this.getEndpoint().getClient().addEventListener(this.handler);
    }

    @Override
    protected void doStop() throws Exception {
        if (this.handler != null) {
            this.getEndpoint().getClient().removeEventListener(this.handler);
            this.handler = null;
        }
        super.doStop();
    }
}
