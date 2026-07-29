package io.meyer1994;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KickConsumer extends DefaultConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(KickConsumer.class);
    private static final URI PUSHER_URI = URI.create(
            "wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679"
                    + "?protocol=7&client=js&version=8.4.0&flash=false");
    private static final String CHAT_MESSAGE_EVENT = "App\\Events\\ChatMessageEvent";

    private final KickEndpoint endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object connectionLock = new Object();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
    private final HttpClient httpClient;

    private ScheduledExecutorService reconnectExecutor;
    private Connection activeConnection;
    private CompletableFuture<WebSocket> pendingOpen;
    private volatile long chatroomId;

    public KickConsumer(KickEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        this.chatroomId = endpoint.getChatroomId() == null ? 0 : endpoint.getChatroomId();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(endpoint.getConnectionTimeout()))
                .build();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        stopping.set(false);
        synchronized (connectionLock) {
            activeConnection = null;
            pendingOpen = null;
        }
        reconnectExecutor = endpoint.getCamelContext()
                .getExecutorServiceManager()
                .newSingleThreadScheduledExecutor(this, endpoint.getEndpointUri());
        connect();
    }

    @Override
    protected void doStop() throws Exception {
        stopping.set(true);

        Connection connection;
        CompletableFuture<WebSocket> open;
        synchronized (connectionLock) {
            connection = activeConnection;
            activeConnection = null;
            open = pendingOpen;
            pendingOpen = null;
        }
        if (open != null) {
            open.cancel(true);
        }
        if (connection != null) {
            connection.abort();
        }

        if (reconnectExecutor != null) {
            endpoint.getCamelContext()
                    .getExecutorServiceManager()
                    .shutdownNow(reconnectExecutor);
            reconnectExecutor = null;
        }
        reconnectScheduled.set(false);
        super.doStop();
    }

    private void connect() {
        if (stopping.get()) {
            return;
        }

        Connection connection = null;
        try {
            if (endpoint.getChatroomId() != null) {
                chatroomId = endpoint.getChatroomId();
            } else {
                chatroomId = resolveChatroomId(endpoint.getChannel());
            }

            LOG.info("Connecting to Kick channel {} using chatroom {}",
                    endpoint.getChannel(), chatroomId);

            connection = new Connection();
            synchronized (connectionLock) {
                if (stopping.get()) {
                    return;
                }
                activeConnection = connection;
            }

            CompletableFuture<WebSocket> open = openWebSocket(connection);
            boolean obsolete;
            synchronized (connectionLock) {
                obsolete = stopping.get() || activeConnection != connection;
                if (!obsolete) {
                    pendingOpen = open;
                }
            }
            if (obsolete) {
                open.cancel(true);
                connection.abort();
                return;
            }
            Connection openedConnection = connection;
            open.whenComplete((socket, error) ->
                    completeConnectionAttempt(openedConnection, open, socket, error));
        } catch (Exception exception) {
            if (connection == null) {
                handleConnectionFailure(exception);
            } else {
                connectionFailed(connection, exception);
            }
        }
    }

    protected long resolveChatroomId(String channel) throws Exception {
        String encodedChannel = URLEncoder.encode(channel, StandardCharsets.UTF_8);
        URI lookupUri = URI.create(
                "https://kick.com/api/v2/channels/" + encodedChannel + "/chatroom");
        HttpRequest request = HttpRequest.newBuilder(lookupUri)
                .timeout(Duration.ofMillis(endpoint.getConnectionTimeout()))
                .header("Accept", "application/json")
                .header("User-Agent", "camel-kick/0.0.2")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Kick chatroom lookup returned HTTP " + response.statusCode()
                            + "; configure chatroomId explicitly if Kick blocks anonymous lookup");
        }
        return parseChatroomId(response.body());
    }

    protected CompletableFuture<WebSocket> openWebSocket(WebSocket.Listener listener) {
        return httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofMillis(endpoint.getConnectionTimeout()))
                .buildAsync(PUSHER_URI, listener);
    }

    void handleWebSocketMessage(WebSocket socket, String rawMessage) {
        handleWebSocketMessage(null, socket, rawMessage);
    }

    private void handleWebSocketMessage(
            Connection connection, WebSocket socket, String rawMessage) {
        try {
            JsonNode envelope = objectMapper.readTree(rawMessage);
            String eventName = envelope.path("event").asText();

            switch (eventName) {
                case "pusher:connection_established" -> subscribe(connection, socket);
                case "pusher:ping" -> sendPongEvent(connection, socket);
                case CHAT_MESSAGE_EVENT -> emitChatMessage(parseChatMessage(envelope.get("data")));
                case "pusher_internal:subscription_succeeded" ->
                    LOG.info("Subscribed to Kick chatroom {}", chatroomId);
                case "pusher:error" -> LOG.warn("Kick Pusher error: {}", envelope.path("data"));
                default -> LOG.trace("Ignoring Kick event {}", eventName);
            }
        } catch (Exception exception) {
            getExceptionHandler().handleException("Could not parse Kick WebSocket message", exception);
        }
    }

    long parseChatroomId(String responseBody) throws Exception {
        JsonNode idNode = objectMapper.readTree(responseBody).get("id");
        if (idNode == null || !idNode.canConvertToLong() || idNode.asLong() <= 0) {
            throw new IllegalArgumentException("Kick chatroom response did not contain a positive numeric id");
        }
        return idNode.asLong();
    }

    private void completeConnectionAttempt(
            Connection connection,
            CompletableFuture<WebSocket> open,
            WebSocket socket,
            Throwable error) {
        boolean current;
        synchronized (connectionLock) {
            current = !stopping.get()
                    && activeConnection == connection
                    && pendingOpen == open;
            if (pendingOpen == open) {
                pendingOpen = null;
            }
            if (current && error == null) {
                connection.setSocketIfAbsent(socket);
                reconnectScheduled.set(false);
            }
        }

        if (!current) {
            if (socket != null) {
                socket.abort();
            }
            return;
        }
        if (error != null) {
            connectionFailed(connection, error);
        }
    }

    private void handleConnectionFailure(Throwable error) {
        getExceptionHandler().handleException(
                "Could not connect to Kick channel " + endpoint.getChannel(), error);
        scheduleReconnect();
    }

    private void connectionFailed(Connection connection, Throwable error) {
        if (!retireConnection(connection, true)) {
            LOG.debug("Ignoring failure from an inactive Kick WebSocket", error);
            return;
        }
        getExceptionHandler().handleException(
                "Could not connect to Kick channel " + endpoint.getChannel(), error);
        scheduleReconnect();
    }

    private void connectionClosed(
            Connection connection, int statusCode, String reason) {
        if (!retireConnection(connection, false)) {
            LOG.debug("Ignoring close from an inactive Kick WebSocket: {} {}",
                    statusCode, reason);
            return;
        }
        LOG.warn("Kick WebSocket closed for channel {}: {} {}",
                endpoint.getChannel(), statusCode, reason);
        scheduleReconnect();
    }

    private void connectionErrored(Connection connection, Throwable error) {
        if (!retireConnection(connection, true)) {
            LOG.debug("Ignoring error from an inactive Kick WebSocket", error);
            return;
        }
        getExceptionHandler().handleException(
                "Kick WebSocket error for channel " + endpoint.getChannel(), error);
        scheduleReconnect();
    }

    private boolean retireConnection(Connection connection, boolean abort) {
        CompletableFuture<WebSocket> open;
        synchronized (connectionLock) {
            if (stopping.get() || activeConnection != connection) {
                return false;
            }
            activeConnection = null;
            open = pendingOpen;
            pendingOpen = null;
        }

        if (open != null) {
            open.cancel(true);
        }
        if (abort) {
            connection.abort();
        } else {
            connection.clearTextBuffer();
        }
        return true;
    }

    private boolean isActive(Connection connection) {
        synchronized (connectionLock) {
            return !stopping.get() && activeConnection == connection;
        }
    }

    private boolean registerSocket(Connection connection, WebSocket socket) {
        synchronized (connectionLock) {
            if (stopping.get() || activeConnection != connection) {
                return false;
            }
            connection.setSocket(socket);
            return true;
        }
    }

    private void scheduleReconnect() {
        if (stopping.get()
                || reconnectExecutor == null
                || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        reconnectExecutor.schedule(() -> {
            reconnectScheduled.set(false);
            connect();
        }, endpoint.getReconnectDelay(), TimeUnit.MILLISECONDS);
    }

    private KickChatMessage parseChatMessage(JsonNode data) throws Exception {
        if (data == null || data.isNull()) {
            throw new IllegalArgumentException("Kick chat message data is required");
        }
        JsonNode event = data.isTextual() ? objectMapper.readTree(data.asText()) : data;
        return objectMapper.treeToValue(event, KickChatMessage.class);
    }

    private void subscribe(Connection connection, WebSocket socket) throws Exception {
        if (chatroomId <= 0) {
            throw new IllegalStateException("Kick chatroom ID has not been resolved");
        }
        String subscription = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("event", "pusher:subscribe")
                        .set("data", objectMapper.createObjectNode()
                                .put("auth", "")
                                .put("channel", "chatrooms." + chatroomId + ".v2")));
        sendProtocolText(
                connection,
                socket,
                subscription,
                "Could not subscribe to Kick chatroom " + chatroomId);
    }

    private void sendPongEvent(Connection connection, WebSocket socket) throws Exception {
        String pong = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("event", "pusher:pong")
                        .set("data", objectMapper.createObjectNode()));
        sendProtocolText(
                connection,
                socket,
                pong,
                "Could not send Kick Pusher pong");
    }

    private void sendProtocolText(
            Connection connection,
            WebSocket socket,
            String message,
            String failureMessage) {
        try {
            observeProtocolSend(
                    connection,
                    socket.sendText(message, true),
                    failureMessage);
        } catch (RuntimeException exception) {
            handleProtocolSendFailure(connection, failureMessage, exception);
        }
    }

    private void observeProtocolSend(
            Connection connection,
            CompletionStage<WebSocket> send,
            String failureMessage) {
        send.whenComplete((ignored, error) -> {
            if (error == null) {
                return;
            }
            handleProtocolSendFailure(connection, failureMessage, error);
        });
    }

    private void handleProtocolSendFailure(
            Connection connection, String failureMessage, Throwable error) {
        if (connection == null) {
            getExceptionHandler().handleException(failureMessage, error);
            return;
        }
        if (!retireConnection(connection, true)) {
            LOG.debug("Ignoring protocol send failure from an inactive Kick WebSocket", error);
            return;
        }
        getExceptionHandler().handleException(failureMessage, error);
        scheduleReconnect();
    }

    private void emitChatMessage(KickChatMessage event) {
        Exchange exchange = endpoint.createExchange(ExchangePattern.InOnly);
        exchange.getMessage().setBody(event);
        exchange.getMessage().setHeader(KickConstants.HEADER_CHANNEL_NAME, endpoint.getChannel());
        exchange.getMessage().setHeader(KickConstants.HEADER_CHATROOM_ID,
                event.chatroomId() == null ? chatroomId : event.chatroomId());
        exchange.getMessage().setHeader(KickConstants.HEADER_USER_ID, event.sender().id());
        exchange.getMessage().setHeader(KickConstants.HEADER_USER_NAME, event.sender().username());
        exchange.getMessage().setHeader(KickConstants.HEADER_MESSAGE_ID, event.id());
        exchange.getMessage().setHeader(KickConstants.HEADER_EVENT_TYPE, KickEvent.CHAT.name());

        try {
            getProcessor().process(exchange);
        } catch (Exception exception) {
            getExceptionHandler().handleException("Error processing Kick chat message", exchange, exception);
        }
    }

    @Override
    public KickEndpoint getEndpoint() {
        return endpoint;
    }

    private final class Connection implements WebSocket.Listener {
        private final StringBuilder textBuffer = new StringBuilder();
        private volatile WebSocket socket;

        @Override
        public void onOpen(WebSocket socket) {
            if (!registerSocket(this, socket)) {
                socket.abort();
                return;
            }
            reconnectScheduled.set(false);
            LOG.info("Kick WebSocket connected for channel {}", endpoint.getChannel());
            socket.request(1);
        }

        @Override
        public CompletionStage<?> onText(
                WebSocket socket, CharSequence data, boolean last) {
            if (!isActive(this)) {
                return null;
            }

            String message = null;
            synchronized (textBuffer) {
                textBuffer.append(data);
                if (last) {
                    message = textBuffer.toString();
                    textBuffer.setLength(0);
                }
            }
            if (message != null) {
                handleWebSocketMessage(this, socket, message);
            }
            if (isActive(this)) {
                socket.request(1);
            }
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket socket, ByteBuffer message) {
            if (!isActive(this)) {
                return null;
            }
            socket.request(1);
            try {
                CompletionStage<WebSocket> pong = socket.sendPong(message);
                observeProtocolSend(this, pong, "Could not answer Kick WebSocket ping");
                return pong;
            } catch (RuntimeException exception) {
                handleProtocolSendFailure(
                        this, "Could not answer Kick WebSocket ping", exception);
                return null;
            }
        }

        @Override
        public CompletionStage<?> onPong(WebSocket socket, ByteBuffer message) {
            if (isActive(this)) {
                socket.request(1);
            }
            return null;
        }

        @Override
        public CompletionStage<?> onClose(
                WebSocket socket, int statusCode, String reason) {
            connectionClosed(this, statusCode, reason);
            return null;
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            connectionErrored(this, error);
        }

        private void setSocketIfAbsent(WebSocket socket) {
            if (this.socket == null) {
                this.socket = socket;
            }
        }

        private void setSocket(WebSocket socket) {
            this.socket = socket;
        }

        private void clearTextBuffer() {
            synchronized (textBuffer) {
                textBuffer.setLength(0);
            }
        }

        private void abort() {
            clearTextBuffer();
            WebSocket currentSocket = socket;
            if (currentSocket != null) {
                currentSocket.abort();
            }
        }
    }
}
