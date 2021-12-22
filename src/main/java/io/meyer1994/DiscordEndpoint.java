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

import java.util.concurrent.ExecutorService;

/**
 * Discord component which does bla bla.
 *
 * TODO: Update one line description above what the component does.
 */
@UriEndpoint(firstVersion = "1.0-SNAPSHOT", scheme = "discord", title = "Discord", syntax="discord:name",
             category = {Category.JAVA})
public class DiscordEndpoint extends DefaultEndpoint {
    @UriPath
    @Metadata(required = true)
    private String name;

    // Producer
    @UriParam(defaultValue = "MESSAGE_SEND")
    private DiscordOperation op;

    // Consumer
    @UriParam(defaultValue = "ON_MESSAGE")
    private DiscordEvent ev;

    public DiscordEndpoint() {
    }

    public DiscordEndpoint(String uri, DiscordComponent component) {
        super(uri, component);
    }

    public Producer createProducer() {
        return new DiscordProducer(this);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        Consumer consumer = new DiscordConsumer(this, processor);
        configureConsumer(consumer);
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

    /**
     * The operation to be executed when used by producer.
     */
    public DiscordOperation getOp() {
        return op;
    }

    public void setOp(DiscordOperation op) {
        this.op = op;
    }

    public ExecutorService createExecutor() {
        // TODO: Delete me when you implemented your custom component
        return getCamelContext()
                .getExecutorServiceManager()
                .newSingleThreadExecutor(this, "DiscordConsumer");
    }
}
