package io.meyer1994;

import static com.github.twitch4j.chat.util.MessageParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.spi.ExceptionHandler;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.IRCMessageEvent;

class TwitchConsumerTest extends CamelTestSupport {

    @Test
    void consumesMatchingChannelMessageWithBodyAndHeaders() throws Exception {
        TestTwitchChat client = new TestTwitchChat();
        TwitchEndpoint endpoint = endpoint("twitch", client);
        AtomicReference<Exchange> received = new AtomicReference<>();
        TwitchConsumer consumer = (TwitchConsumer) endpoint.createConsumer(received::set);
        ChannelMessageEvent event = messageEvent("twitch", "42", "viewer", "7", "Hello chat!", "message-1");

        consumer.start();
        client.getEventManager().publish(event);

        Exchange exchange = received.get();
        assertSame(event, exchange.getMessage().getBody());
        assertEquals(ExchangePattern.InOnly, exchange.getPattern());
        assertEquals("42", exchange.getMessage().getHeader("x-camel-twitch-channel-id"));
        assertEquals("twitch", exchange.getMessage().getHeader("x-camel-twitch-channel-name"));
        assertEquals("7", exchange.getMessage().getHeader("x-camel-twitch-user-id"));
        assertEquals("viewer", exchange.getMessage().getHeader("x-camel-twitch-user-name"));
        assertEquals("message-1", exchange.getMessage().getHeader("x-camel-twitch-message-id"));
    }

    @Test
    void ignoresMessagesFromOtherChannels() throws Exception {
        TestTwitchChat client = new TestTwitchChat();
        TwitchEndpoint endpoint = endpoint("twitch", client);
        AtomicInteger received = new AtomicInteger();
        TwitchConsumer consumer = (TwitchConsumer) endpoint.createConsumer(exchange -> received.incrementAndGet());

        consumer.start();
        client.getEventManager().publish(messageEvent("someone_else", "8", "viewer", "7", "Wrong room", "message-2"));

        assertEquals(0, received.get());
    }

    @Test
    void joinsOnStartAndDisposesListenerAndLeavesOnStop() throws Exception {
        TestTwitchChat client = new TestTwitchChat();
        TwitchEndpoint endpoint = endpoint("twitch", client);
        AtomicInteger received = new AtomicInteger();
        TwitchConsumer consumer = (TwitchConsumer) endpoint.createConsumer(exchange -> received.incrementAndGet());
        ChannelMessageEvent event = messageEvent("twitch", "42", "viewer", "7", "Hello", "message-3");

        consumer.start();
        assertTrue(client.getChannels().contains("twitch"));
        client.getEventManager().publish(event);
        assertEquals(1, received.get());

        consumer.stop();
        assertFalse(client.getChannels().contains("twitch"));
        client.getEventManager().publish(event);
        assertEquals(1, received.get());
        assertFalse(client.isClosed());
    }

    @Test
    void delegatesProcessorFailuresToConsumerExceptionHandler() throws Exception {
        TestTwitchChat client = new TestTwitchChat();
        TwitchEndpoint endpoint = endpoint("twitch", client);
        TwitchConsumer consumer = (TwitchConsumer) endpoint.createConsumer(exchange -> {
            throw new IllegalArgumentException("route failed");
        });
        RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
        consumer.setExceptionHandler(exceptionHandler);

        consumer.start();
        client.getEventManager().publish(messageEvent("twitch", "42", "viewer", "7", "Hello", "message-4"));

        assertEquals("Error processing Twitch chat message", exceptionHandler.message.get());
        assertEquals("route failed", exceptionHandler.failure.get().getMessage());
        assertEquals("message-4",
                exceptionHandler.exchange.get().getMessage().getHeader("x-camel-twitch-message-id"));
    }

    private TwitchEndpoint endpoint(String channel, TestTwitchChat client) {
        TwitchEndpoint endpoint = context.getEndpoint("twitch:" + channel + "?event=CHAT", TwitchEndpoint.class);
        endpoint.setClient(client.asClient());
        return endpoint;
    }

    private ChannelMessageEvent messageEvent(
            String channelName,
            String channelId,
            String userName,
            String userId,
            String message,
            String messageId) {
        IRCMessageEvent raw = parse("@badge-info=;badges=;color=#0000FF;display-name=" + userName
                + ";emotes=;first-msg=0;flags=;id=" + messageId
                + ";mod=0;room-id=" + channelId
                + ";subscriber=0;tmi-sent-ts=1643904084794;turbo=0;user-id=" + userId
                + ";user-type= :" + userName + "!" + userName + "@" + userName
                + ".tmi.twitch.tv PRIVMSG #" + channelName + " :" + message);
        return new ChannelMessageEvent(raw.getChannel(), raw, raw.getUser(), raw.getMessage().orElseThrow());
    }

    private static final class RecordingExceptionHandler implements ExceptionHandler {
        private final AtomicReference<String> message = new AtomicReference<>();
        private final AtomicReference<Exchange> exchange = new AtomicReference<>();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        @Override
        public void handleException(Throwable exception) {
            failure.set(exception);
        }

        @Override
        public void handleException(String message, Throwable exception) {
            this.message.set(message);
            failure.set(exception);
        }

        @Override
        public void handleException(String message, Exchange exchange, Throwable exception) {
            this.message.set(message);
            this.exchange.set(exchange);
            failure.set(exception);
        }
    }
}
