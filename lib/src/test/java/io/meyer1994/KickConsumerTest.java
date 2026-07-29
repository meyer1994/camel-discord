package io.meyer1994;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.spi.ExceptionHandler;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

class KickConsumerTest extends CamelTestSupport {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void emitsTypedChatMessageAndHeadersFromDoubleEncodedData() throws Exception {
        AtomicReference<Exchange> received = new AtomicReference<>();
        KickConsumer consumer = consumer(received::set);
        TestWebSocket socket = new TestWebSocket();

        consumer.handleWebSocketMessage(socket, chatEnvelope(chatPayload(), true));

        Exchange exchange = received.get();
        assertEquals(ExchangePattern.InOnly, exchange.getPattern());
        KickChatMessage body = assertInstanceOf(KickChatMessage.class, exchange.getMessage().getBody());
        assertEquals("message-1", body.id());
        assertEquals(668L, body.chatroomId());
        assertEquals("Hello chat!", body.content());
        assertEquals("message", body.type());
        assertEquals("2026-07-28T22:00:00Z", body.createdAt());
        assertEquals(new KickChatSender(42L, "Viewer", "viewer"), body.sender());
        assertEquals("xqc", exchange.getMessage().getHeader(KickConstants.HEADER_CHANNEL_NAME));
        assertEquals(668L, exchange.getMessage().getHeader(KickConstants.HEADER_CHATROOM_ID));
        assertEquals(42L, exchange.getMessage().getHeader(KickConstants.HEADER_USER_ID));
        assertEquals("Viewer", exchange.getMessage().getHeader(KickConstants.HEADER_USER_NAME));
        assertEquals("message-1", exchange.getMessage().getHeader(KickConstants.HEADER_MESSAGE_ID));
        assertEquals("CHAT", exchange.getMessage().getHeader(KickConstants.HEADER_EVENT_TYPE));
    }

    @Test
    void acceptsObjectValuedPusherData() throws Exception {
        AtomicReference<Exchange> received = new AtomicReference<>();
        KickConsumer consumer = consumer(received::set);

        consumer.handleWebSocketMessage(new TestWebSocket(), chatEnvelope(chatPayload(), false));

        KickChatMessage body = received.get().getMessage().getBody(KickChatMessage.class);
        assertEquals("Hello chat!", body.content());
    }

    @Test
    void waitsForFinalTextFragmentBeforeEmitting() throws Exception {
        AtomicReference<Exchange> received = new AtomicReference<>();
        KickEndpoint endpoint = context.getEndpoint("kick:xqc?chatroomId=668", KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, received::set, 777L);
        String envelope = chatEnvelope(chatPayload(), true);
        int split = envelope.length() / 2;

        consumer.start();
        try {
            WebSocket.Listener listener = consumer.listener(0);
            TestWebSocket socket = consumer.socket(0);
            listener.onText(socket, envelope.substring(0, split), false);
            assertNull(received.get());
            listener.onText(socket, envelope.substring(split), true);

            assertEquals("message-1", received.get().getMessage().getBody(KickChatMessage.class).id());
            assertEquals(3, socket.requests.get());
        } finally {
            consumer.stop();
        }
    }

    @Test
    void subscribesToResolvedChatroomAfterPusherConnects() throws Exception {
        KickConsumer consumer = consumer(exchange -> {
        });
        TestWebSocket socket = new TestWebSocket();

        consumer.handleWebSocketMessage(socket,
                OBJECT_MAPPER.writeValueAsString(Map.of("event", "pusher:connection_established", "data", "{}")));

        JsonNode subscription = OBJECT_MAPPER.readTree(socket.sentTexts.getFirst());
        assertEquals("pusher:subscribe", subscription.path("event").asText());
        assertEquals("", subscription.path("data").path("auth").asText());
        assertEquals("chatrooms.668.v2", subscription.path("data").path("channel").asText());
    }

    @Test
    void answersPusherPing() throws Exception {
        KickConsumer consumer = consumer(exchange -> {
        });
        TestWebSocket socket = new TestWebSocket();

        consumer.handleWebSocketMessage(socket,
                OBJECT_MAPPER.writeValueAsString(Map.of("event", "pusher:ping", "data", "{}")));

        JsonNode pong = OBJECT_MAPPER.readTree(socket.sentTexts.getFirst());
        assertEquals("pusher:pong", pong.path("event").asText());
    }

    @Test
    void ignoresUnrelatedPusherEvents() throws Exception {
        AtomicInteger received = new AtomicInteger();
        KickConsumer consumer = consumer(exchange -> received.incrementAndGet());

        consumer.handleWebSocketMessage(new TestWebSocket(),
                OBJECT_MAPPER.writeValueAsString(Map.of("event", "App\\Events\\FollowEvent", "data", "{}")));

        assertEquals(0, received.get());
    }

    @Test
    void reportsMalformedMessagesToExceptionHandler() {
        KickConsumer consumer = consumer(exchange -> {
        });
        RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
        consumer.setExceptionHandler(exceptionHandler);

        consumer.handleWebSocketMessage(new TestWebSocket(), "{not-json");

        assertEquals("Could not parse Kick WebSocket message", exceptionHandler.message.get());
        assertInstanceOf(Exception.class, exceptionHandler.failure.get());
    }

    @Test
    void reportsMissingRequiredChatFieldsToExceptionHandler() throws Exception {
        KickConsumer consumer = consumer(exchange -> {
        });
        RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
        consumer.setExceptionHandler(exceptionHandler);
        String incomplete = """
                {"id":"message-1","chatroom_id":668,"content":"Hello"}
                """;

        consumer.handleWebSocketMessage(new TestWebSocket(), chatEnvelope(incomplete, true));

        assertEquals("Could not parse Kick WebSocket message", exceptionHandler.message.get());
        assertTrue(exceptionHandler.failure.get().getMessage().contains("sender"));
    }

    @Test
    void delegatesProcessorFailuresToExceptionHandler() throws Exception {
        KickConsumer consumer = consumer(exchange -> {
            throw new IllegalArgumentException("route failed");
        });
        RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
        consumer.setExceptionHandler(exceptionHandler);

        consumer.handleWebSocketMessage(new TestWebSocket(), chatEnvelope(chatPayload(), true));

        assertEquals("Error processing Kick chat message", exceptionHandler.message.get());
        assertEquals("route failed", exceptionHandler.failure.get().getMessage());
        assertEquals("message-1",
                exceptionHandler.exchange.get().getMessage().getHeader(KickConstants.HEADER_MESSAGE_ID));
    }

    @Test
    void parsesChatroomLookupResponse() throws Exception {
        KickConsumer consumer = consumer(exchange -> {
        });

        assertEquals(668L, consumer.parseChatroomId("""
                {"id":668,"channel_id":123,"chat_mode":"public"}
                """));
    }

    @Test
    void requestsFirstWebSocketMessageOnOpen() throws Exception {
        KickEndpoint endpoint = context.getEndpoint("kick:xqc?chatroomId=668", KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);

        consumer.start();
        try {
            assertEquals(1, consumer.socket(0).requests.get());
        } finally {
            consumer.stop();
        }
    }

    @Test
    void resolvesChatroomAndConnectsWhenStarted() throws Exception {
        KickEndpoint endpoint = context.getEndpoint("kick:xqc?reconnectDelay=20", KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);

        consumer.start();
        try {
            assertEquals(1, consumer.resolveCalls.get());
            assertEquals(1, consumer.openCalls.get());

            consumer.handleWebSocketMessage(consumer.socket(0),
                    OBJECT_MAPPER.writeValueAsString(Map.of(
                            "event", "pusher:connection_established",
                            "data", "{}")));

            JsonNode subscription = OBJECT_MAPPER.readTree(consumer.socket(0).sentTexts.getFirst());
            assertEquals("chatrooms.777.v2", subscription.path("data").path("channel").asText());
        } finally {
            consumer.stop();
        }
    }

    @Test
    void skipsLookupWhenChatroomIdIsConfigured() throws Exception {
        KickEndpoint endpoint = context.getEndpoint("kick:xqc?chatroomId=668", KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);

        consumer.start();
        try {
            assertEquals(0, consumer.resolveCalls.get());
            assertEquals(1, consumer.openCalls.get());
        } finally {
            consumer.stop();
        }
    }

    @Test
    void closesWebSocketWhenStopped() throws Exception {
        KickEndpoint endpoint = context.getEndpoint("kick:xqc?chatroomId=668", KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);

        consumer.start();
        consumer.stop();

        assertTrue(consumer.socket(0).isOutputClosed());
    }

    @Test
    void schedulesOnlyOneReconnectForDuplicateCloseCallbacks() throws Exception {
        KickEndpoint endpoint = context.getEndpoint(
                "kick:xqc?chatroomId=668&reconnectDelay=20",
                KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);

        consumer.start();
        try {
            WebSocket.Listener listener = consumer.listener(0);
            TestWebSocket socket = consumer.socket(0);
            listener.onClose(socket, 1006, "lost");
            listener.onClose(socket, 1006, "lost again");

            assertTrue(consumer.secondOpen.await(1, TimeUnit.SECONDS));
            assertEquals(2, consumer.openCalls.get());
        } finally {
            consumer.stop();
        }
    }

    @Test
    void ignoresLateFailureFromReplacedSocket() throws Exception {
        KickEndpoint endpoint = context.getEndpoint(
                "kick:xqc?chatroomId=668&reconnectDelay=20",
                KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);

        consumer.start();
        try {
            WebSocket.Listener firstListener = consumer.listener(0);
            TestWebSocket firstSocket = consumer.socket(0);
            firstListener.onClose(firstSocket, 1006, "lost");
            assertTrue(consumer.secondOpen.await(1, TimeUnit.SECONDS));

            firstListener.onError(firstSocket, new IllegalStateException("late failure"));

            assertFalse(consumer.thirdOpen.await(150, TimeUnit.MILLISECONDS));
            assertEquals(2, consumer.openCalls.get());
        } finally {
            consumer.stop();
        }

        assertTrue(consumer.socket(1).isOutputClosed());
    }

    @Test
    void discardsPartialTextWhenAConnectionIsReplaced() throws Exception {
        AtomicReference<Exchange> received = new AtomicReference<>();
        KickEndpoint endpoint = context.getEndpoint(
                "kick:xqc?chatroomId=668&reconnectDelay=20",
                KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, received::set, 777L);
        String envelope = chatEnvelope(chatPayload(), true);

        consumer.start();
        try {
            WebSocket.Listener firstListener = consumer.listener(0);
            TestWebSocket firstSocket = consumer.socket(0);
            firstListener.onText(firstSocket, envelope.substring(0, envelope.length() / 2), false);
            firstListener.onClose(firstSocket, 1006, "lost between fragments");
            assertTrue(consumer.secondOpen.await(1, TimeUnit.SECONDS));

            consumer.listener(1).onText(consumer.socket(1), envelope, true);

            assertEquals("message-1",
                    received.get().getMessage().getBody(KickChatMessage.class).id());
        } finally {
            consumer.stop();
        }
    }

    @Test
    void cancelsPendingWebSocketHandshakeWhenStopped() throws Exception {
        KickEndpoint endpoint = context.getEndpoint(
                "kick:xqc?chatroomId=668",
                KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);
        consumer.completeOpens = false;

        consumer.start();
        CompletableFuture<WebSocket> pendingOpen = consumer.pendingOpen;
        assertFalse(pendingOpen.isDone());

        consumer.stop();

        assertTrue(pendingOpen.isCancelled());
    }

    @Test
    void reconnectsWhenSubscriptionSendFails() throws Exception {
        KickEndpoint endpoint = context.getEndpoint(
                "kick:xqc?chatroomId=668&reconnectDelay=20",
                KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);
        RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
        consumer.setExceptionHandler(exceptionHandler);

        consumer.start();
        try {
            TestWebSocket firstSocket = consumer.socket(0);
            firstSocket.sendTextFailure = new IllegalStateException("send failed");
            consumer.listener(0).onText(
                    firstSocket,
                    OBJECT_MAPPER.writeValueAsString(Map.of(
                            "event", "pusher:connection_established",
                            "data", "{}")),
                    true);

            assertTrue(consumer.secondOpen.await(1, TimeUnit.SECONDS));
            assertEquals("Could not subscribe to Kick chatroom 668", exceptionHandler.message.get());
            assertEquals("send failed", exceptionHandler.failure.get().getMessage());
            assertTrue(firstSocket.isOutputClosed());
        } finally {
            consumer.stop();
        }
    }

    @Test
    void reconnectsWhenSubscriptionSendThrows() throws Exception {
        KickEndpoint endpoint = context.getEndpoint(
                "kick:xqc?chatroomId=668&reconnectDelay=20",
                KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);
        RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
        consumer.setExceptionHandler(exceptionHandler);

        consumer.start();
        try {
            TestWebSocket firstSocket = consumer.socket(0);
            firstSocket.throwSendTextFailure = new IllegalStateException("send still pending");
            consumer.listener(0).onText(
                    firstSocket,
                    OBJECT_MAPPER.writeValueAsString(Map.of(
                            "event", "pusher:connection_established",
                            "data", "{}")),
                    true);

            assertTrue(consumer.secondOpen.await(1, TimeUnit.SECONDS));
            assertEquals("Could not subscribe to Kick chatroom 668", exceptionHandler.message.get());
            assertEquals("send still pending", exceptionHandler.failure.get().getMessage());
            assertTrue(firstSocket.isOutputClosed());
        } finally {
            consumer.stop();
        }
    }

    @Test
    void abortsSocketWhoseOpenCallbackArrivesAfterStop() throws Exception {
        KickEndpoint endpoint = context.getEndpoint(
                "kick:xqc?chatroomId=668",
                KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);
        consumer.completeOpens = false;

        consumer.start();
        WebSocket.Listener listener = consumer.listener(0);
        TestWebSocket socket = consumer.socket(0);
        consumer.stop();

        listener.onOpen(socket);

        assertTrue(socket.isOutputClosed());
        assertEquals(0, socket.requests.get());
    }

    @Test
    void reportsLookupFailureThroughConsumerExceptionHandler() throws Exception {
        KickEndpoint endpoint = context.getEndpoint(
                "kick:xqc?reconnectDelay=60000",
                KickEndpoint.class);
        TestKickConsumer consumer = new TestKickConsumer(endpoint, exchange -> {
        }, 777L);
        consumer.lookupFailure = new IllegalStateException("Kick chatroom lookup returned HTTP 403");
        RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
        consumer.setExceptionHandler(exceptionHandler);

        consumer.start();
        try {
            assertEquals("Could not connect to Kick channel xqc", exceptionHandler.message.get());
            assertSame(consumer.lookupFailure, exceptionHandler.failure.get());
            assertEquals(0, consumer.openCalls.get());
        } finally {
            consumer.stop();
        }
    }

    private KickConsumer consumer(Processor processor) {
        KickEndpoint endpoint = context.getEndpoint("kick:xqc?chatroomId=668", KickEndpoint.class);
        return new KickConsumer(endpoint, processor);
    }

    private static String chatPayload() {
        return """
                {
                  "id": "message-1",
                  "chatroom_id": 668,
                  "content": "Hello chat!",
                  "type": "message",
                  "created_at": "2026-07-28T22:00:00Z",
                  "sender": {
                    "id": 42,
                    "username": "Viewer",
                    "slug": "viewer",
                    "identity": {
                      "color": "#00ff00",
                      "badges": [{"type":"subscriber","text":"Subscriber"}]
                    }
                  },
                  "metadata": {
                    "original_sender": {"id":"7","username":"Other"}
                  }
                }
                """;
    }

    private static String chatEnvelope(String payload, boolean encodeDataAsText) throws Exception {
        Object data = encodeDataAsText ? payload : OBJECT_MAPPER.readTree(payload);
        return OBJECT_MAPPER.writeValueAsString(Map.of(
                "event", "App\\Events\\ChatMessageEvent",
                "channel", "chatrooms.668.v2",
                "data", data));
    }

    private static final class TestWebSocket implements WebSocket {
        private final AtomicInteger requests = new AtomicInteger();
        private final List<String> sentTexts = new ArrayList<>();
        private boolean inputClosed;
        private boolean outputClosed;
        private RuntimeException sendTextFailure;
        private RuntimeException throwSendTextFailure;

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            sentTexts.add(data.toString());
            if (throwSendTextFailure != null) {
                throw throwSendTextFailure;
            }
            if (sendTextFailure != null) {
                return CompletableFuture.failedFuture(sendTextFailure);
            }
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            outputClosed = true;
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
            requests.addAndGet((int) n);
        }

        @Override
        public String getSubprotocol() {
            return "";
        }

        @Override
        public boolean isOutputClosed() {
            return outputClosed;
        }

        @Override
        public boolean isInputClosed() {
            return inputClosed;
        }

        @Override
        public void abort() {
            inputClosed = true;
            outputClosed = true;
        }
    }

    private static final class TestKickConsumer extends KickConsumer {
        private final long resolvedChatroomId;
        private final AtomicInteger resolveCalls = new AtomicInteger();
        private final AtomicInteger openCalls = new AtomicInteger();
        private final CountDownLatch secondOpen = new CountDownLatch(1);
        private final CountDownLatch thirdOpen = new CountDownLatch(1);
        private final List<TestWebSocket> sockets = new CopyOnWriteArrayList<>();
        private final List<WebSocket.Listener> listeners = new CopyOnWriteArrayList<>();
        private boolean completeOpens = true;
        private CompletableFuture<WebSocket> pendingOpen;
        private RuntimeException lookupFailure;

        private TestKickConsumer(KickEndpoint endpoint, Processor processor, long resolvedChatroomId) {
            super(endpoint, processor);
            this.resolvedChatroomId = resolvedChatroomId;
        }

        @Override
        protected long resolveChatroomId(String channel) {
            resolveCalls.incrementAndGet();
            if (lookupFailure != null) {
                throw lookupFailure;
            }
            return resolvedChatroomId;
        }

        @Override
        protected CompletableFuture<WebSocket> openWebSocket(WebSocket.Listener listener) {
            int call = openCalls.incrementAndGet();
            TestWebSocket socket = new TestWebSocket();
            sockets.add(socket);
            listeners.add(listener);
            if (!completeOpens) {
                pendingOpen = new CompletableFuture<>();
                return pendingOpen;
            }
            listener.onOpen(socket);
            CompletableFuture<WebSocket> open = CompletableFuture.completedFuture(socket);
            if (call == 2) {
                secondOpen.countDown();
            } else if (call == 3) {
                thirdOpen.countDown();
            }
            return open;
        }

        private TestWebSocket socket(int index) {
            return sockets.get(index);
        }

        private WebSocket.Listener listener(int index) {
            return listeners.get(index);
        }
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
