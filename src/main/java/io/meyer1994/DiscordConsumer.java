package io.meyer1994;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

public class DiscordConsumer extends DefaultConsumer {
    private final DiscordEndpoint endpoint;

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
        DiscordEvent event = this.getEndpoint().getEvent();
        if (event == DiscordEvent.ON_MESSAGE)
            this.getEndpoint()
                    .getClient()
                    .addEventListener(new DiscordHandler(this));
    }
}
