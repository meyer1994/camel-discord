package io.meyer1994;

import net.dv8tion.jda.api.JDA;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Discord component which does bla bla.
 *
 * TODO: Update one line description above what the component does.
 */
@UriEndpoint(firstVersion = "1.0-SNAPSHOT", scheme = "discord", title = "Discord", syntax="discord:name",
             category = {Category.JAVA})
public class DiscordEndpoint extends DefaultEndpoint {
    @UriParam
    @Metadata(autowired = true)
    private JDA client;

    @UriPath
    @Metadata(required = true)
    private String name;

    // Producer
    @UriParam(defaultValue = "MESSAGE_SEND")
    private DiscordOperation operation;

    // Consumer
    @UriParam(defaultValue = "ON_MESSAGE")
    private DiscordEvent event;

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
     * The name has no functionality. It is here mostly to
     * make route identification easier for the developer
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public DiscordOperation getOperation() {
        return operation;
    }

    /**
     * The operation to be executed when used by producer.
     */
    public void setOperation(DiscordOperation operation) {
        this.operation = operation;
    }

    public DiscordEvent getEvent() {
        return event;
    }

    /**
     * The type of event that the route listens to.
     */
    public void setEvent(DiscordEvent event) {
        this.event = event;
    }

    public JDA getClient() {
        return client;
    }

    /**
     * The discord JDA client to be used. Must be autowired
     */
    public void setClient(JDA client) {
        this.client = client;
    }
}
