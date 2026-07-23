package io.meyer1994;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

public class DiscordConsumer extends DefaultConsumer {
    private final DiscordEndpoint endpoint;
    private DiscordHandler handler;

    public DiscordConsumer(DiscordEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    public DiscordEndpoint getEndpoint() {
        return (DiscordEndpoint) super.getEndpoint();
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
