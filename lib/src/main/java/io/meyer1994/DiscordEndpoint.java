package io.meyer1994;

import net.dv8tion.jda.api.JDA;

import java.util.Set;

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
 * Apache Camel endpoint for receiving Discord messages and sending, replying to,
 * or reacting to Discord messages through a configured JDA client.
 */
@UriEndpoint(firstVersion = "1.0-SNAPSHOT", scheme = "discord", title = "Discord", syntax="discord:name",
             category = {Category.SOCIAL})
public class DiscordEndpoint extends DefaultEndpoint {
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

    private static final Set<String> PRODUCER_NAMES = Set.of(
            "sendMessage",
            "editMessageById",
            "deleteMessageById",
            "deleteMessagesByIds",
            "retrieveMessageById",
            "addReactionById",
            "removeReactionById",
            "clearReactions",
            "pin",
            "unpin",
            "sendTyping",
            "reply"
    );


    @UriParam
    @Metadata(autowired = true)
    private JDA client;

    @UriPath
    @Metadata(required = true)
    private String name;

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
        switch (this.name) {
            case "onMessageReceived":
            case "onMessageUpdate":
            case "onMessageDelete":
            case "onMessageBulkDelete":
            case "onMessageReactionAdd":
            case "onMessageReactionRemove":
            case "onMessageReactionRemoveAll":
            case "onMessageReactionRemoveEmoji":
                break;
            default:
                throw new IllegalArgumentException("Unsupported Discord listener method: " + this.name);
        }
        Consumer consumer = new DiscordConsumer(this, processor);
        this.configureConsumer(consumer);
        return consumer;
    }

    /**
     * For consumers, the exact ListenerAdapter method name to receive.
     * For producers, the Discord JDA method name to invoke.
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public DiscordOperation getOperation() {
        try {
            return DiscordOperation.valueOf(this.name);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported Discord producer method: " + this.name, exception);
        }
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
