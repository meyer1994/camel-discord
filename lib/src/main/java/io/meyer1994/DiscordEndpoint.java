package io.meyer1994;

import java.util.concurrent.ExecutorService;

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
 * Apache Camel endpoint for receiving Discord messages and sending, replying
 * to,
 * or reacting to Discord messages through a configured JDA client.
 */
@UriEndpoint(firstVersion = "1.0-SNAPSHOT", scheme = "discord", title = "Discord", syntax = "discord:name", category = {
        Category.SOCIAL }, headersClass = DiscordConstants.class)
public class DiscordEndpoint extends DefaultEndpoint {
    @UriParam
    @Metadata(autowired = true)
    protected JDA client;

    @UriPath(description = "Endpoint name")
    @Metadata(required = true)
    protected String name;

    @UriParam(label = "consumer", description = "Discord listener event to consume")
    protected DiscordEvent event;

    @UriParam(label = "consumer,advanced", description = "Use a worker pool to process Discord events in parallel", defaultValue = "true")
    protected boolean consumerWorkerPoolEnabled = true;

    @UriParam(label = "consumer,advanced", description = "Core thread pool size for Discord event processing", defaultValue = "2")
    protected int consumerWorkerPoolSize = 2;

    @UriParam(label = "consumer,advanced", description = "Maximum thread pool size for Discord event processing", defaultValue = "2")
    protected int consumerWorkerPoolMaxSize = 2;

    @UriParam(label = "consumer,advanced", description = "Custom thread pool for processing Discord events")
    protected ExecutorService consumerWorkerPoolExecutorService;

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
        DiscordConsumer answer = new DiscordConsumer(this, processor);
        Consumer consumer = answer;
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

    protected ExecutorService createExecutorService(Object source) {
        return getCamelContext().getExecutorServiceManager().newThreadPool(source, "DiscordConsumerWorker",
                consumerWorkerPoolSize, consumerWorkerPoolMaxSize);
    }

    public boolean isConsumerWorkerPoolEnabled() {
        return consumerWorkerPoolEnabled;
    }

    public void setConsumerWorkerPoolEnabled(boolean consumerWorkerPoolEnabled) {
        this.consumerWorkerPoolEnabled = consumerWorkerPoolEnabled;
    }

    public ExecutorService getConsumerWorkerPoolExecutorService() {
        return consumerWorkerPoolExecutorService;
    }

    public void setConsumerWorkerPoolExecutorService(ExecutorService consumerWorkerPoolExecutorService) {
        this.consumerWorkerPoolExecutorService = consumerWorkerPoolExecutorService;
    }

    public int getConsumerWorkerPoolSize() {
        return consumerWorkerPoolSize;
    }

    public void setConsumerWorkerPoolSize(int consumerWorkerPoolSize) {
        this.consumerWorkerPoolSize = consumerWorkerPoolSize;
    }

    public int getConsumerWorkerPoolMaxSize() {
        return consumerWorkerPoolMaxSize;
    }

    public void setConsumerWorkerPoolMaxSize(int consumerWorkerPoolMaxSize) {
        this.consumerWorkerPoolMaxSize = consumerWorkerPoolMaxSize;
    }

    public DiscordOperation getOperation() {
        return operation;
    }

    public void setOperation(DiscordOperation operation) {
        this.operation = operation;
    }
}
