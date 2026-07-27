package io.meyer1994.example;

import static com.github.twitch4j.chat.util.MessageParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.IRCMessageEvent;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class TwitchStreamServiceTest {
    private CamelContext context;
    private TestTwitchChat client;
    private Scheduler routeScheduler;
    private TwitchStreamService service;

    @BeforeEach
    void setUp() throws Exception {
        context = new DefaultCamelContext();
        client = new TestTwitchChat();
        context.getRegistry().bind("client", client);
        ChatMessagePublisher publisher = new ChatMessagePublisher();
        context.getRegistry().bind("chatMessagePublisher", publisher);
        context.addRoutes(new TwitchChatRouteTemplate());
        context.start();
        routeScheduler = Schedulers.newSingle("test-route-lifecycle");
        service = new TwitchStreamService(context, publisher, routeScheduler);
    }

    @AfterEach
    void tearDown() {
        context.stop();
        client.close();
        routeScheduler.dispose();
    }

    @Test
    void streamsMessagesFromADynamicRouteAndRemovesItOnCancellation() {
        StepVerifier.create(service.stream("CellBit"))
                .then(() -> {
                    await(() -> context.getRoutes().size() == 1
                            && client.getChannels().contains("cellbit"));
                    assertEquals(1, context.getRoutes().size());
                    assertTrue(context.getRoutes().getFirst().getId().startsWith("twitch-sse-cellbit-"));
                    assertTrue(client.getChannels().contains("cellbit"));
                })
                .then(() -> client.getEventManager().publish(
                        messageEvent("cellbit", "42", "viewer", "7", "<Hello & \"chat\">", "message-1")))
                .assertNext(event -> {
                    assertEquals("chat", event.event());
                    assertEquals("message-1", event.id());
                    assertEquals(
                            "<li class=\"px-5 py-4\"><span class=\"font-semibold text-violet-300\">"
                                    + "viewer:</span> &lt;Hello &amp; &quot;chat&quot;&gt;</li>",
                            event.data());
                })
                .thenCancel()
                .verify();

        await(() -> context.getRoutes().isEmpty());
        assertTrue(context.getRoutes().isEmpty());
        assertTrue(client.getChannels().isEmpty());
    }

    @Test
    void rejectsInvalidChannelBeforeCreatingARoute() {
        ResponseStatusException failure = assertThrows(
                ResponseStatusException.class,
                () -> service.stream("not?a-channel"));

        assertEquals(HttpStatus.BAD_REQUEST, failure.getStatusCode());
        assertNull(context.getRoute("twitch-sse-not?a-channel"));
        assertTrue(context.getRoutes().isEmpty());
    }

    private void await(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("Condition was not met before timeout");
            }
            LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
        }
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
}
