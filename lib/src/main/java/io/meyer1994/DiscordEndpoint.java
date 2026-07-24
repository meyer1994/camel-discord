package io.meyer1994;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

import net.dv8tion.jda.api.JDA;

/**
 * Apache Camel endpoint for receiving Discord messages and sending, replying to,
 * or reacting to Discord messages through a configured JDA client.
 */
@UriEndpoint(firstVersion = "1.0-SNAPSHOT", scheme = "discord", title = "Discord", syntax = "discord:name", category = {
        Category.SOCIAL })
public class DiscordEndpoint extends DefaultEndpoint {
    @UriParam
    @Metadata(autowired = true)
    protected JDA client;

    @UriPath(description = "Endpoint name")
    @Metadata(required = true)
    protected String name;

    @UriParam(label = "consumer", description = "Discord listener event to consume")
    protected DiscordEvent event;

    @UriParam(label = "producer", description = "Discord operation to execute")
    protected DiscordOperation operation;

    public DiscordEndpoint() {
    }

    public DiscordEndpoint(String uri, DiscordComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() {
        return new DiscordProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        Consumer consumer = new DiscordConsumer(this, processor);
        this.configureConsumer(consumer);
        return consumer;
    }

    /**
     * The discord JDA client to be used. Must be autowired
     */
    public void setClient(JDA client) {
        this.client = client;
    }

    public JDA getClient() {
        return client;
    }

    public String getName() {
        return name;
    }

    public DiscordEvent getEvent() {
        return event;
    }

    public void setEvent(DiscordEvent event) {
        this.event = event;
    }

    public DiscordOperation getOperation() {
        return operation;
    }

    public void setOperation(DiscordOperation operation) {
        this.operation = operation;
    }
}
