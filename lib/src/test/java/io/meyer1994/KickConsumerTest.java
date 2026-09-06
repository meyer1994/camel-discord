package io.meyer1994;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.PusherEvent;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.spi.ExceptionHandler;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

class KickConsumerTest extends CamelTestSupport {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String CHAT_EVENT = "App\\Events\\ChatMessageEvent";

  @Test
  void emitsTypedChatMessageAndHeadersFromPusherEvent() throws Exception {
    AtomicReference<Exchange> received = new AtomicReference<>();
    KickConsumer consumer = consumer(received::set);
    KickHandler handler = new KickHandler(consumer);

    handler.onChatEvent(chatEvent(chatPayload(), true));

    Exchange exchange = received.get();
    assertEquals(ExchangePattern.InOnly, exchange.getPattern());
    KickChatMessage body = assertInstanceOf(KickChatMessage.class, exchange.getMessage().getBody());
    assertEquals("message-1", body.id());
    assertEquals("Hello chat!", body.content());
    assertEquals("Viewer", body.sender().username());
    assertEquals("xqc", exchange.getMessage().getHeader(KickConstants.HEADER_CHANNEL_NAME));
    assertEquals(668L, exchange.getMessage().getHeader(KickConstants.HEADER_CHATROOM_ID));
    assertEquals(42L, exchange.getMessage().getHeader(KickConstants.HEADER_USER_ID));
    assertEquals("message-1", exchange.getMessage().getHeader(KickConstants.HEADER_MESSAGE_ID));
    assertEquals("CHAT", exchange.getMessage().getHeader(KickConstants.HEADER_EVENT_TYPE));
  }

  @Test
  void acceptsObjectValuedPusherData() throws Exception {
    AtomicReference<Exchange> received = new AtomicReference<>();
    KickConsumer consumer = consumer(received::set);
    KickHandler handler = new KickHandler(consumer);

    handler.onChatEvent(chatEvent(chatPayload(), false));

    assertEquals(
        "Hello chat!", received.get().getMessage().getBody(KickChatMessage.class).content());
  }

  @Test
  void reportsMalformedPusherDataToExceptionHandler() throws Exception {
    KickConsumer consumer = consumer(exchange -> {});
    KickHandler handler = new KickHandler(consumer);
    RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
    consumer.setExceptionHandler(exceptionHandler);

    handler.onChatEvent(
        PusherEvent.fromJson(
            """
                {"event":"App\\\\Events\\\\ChatMessageEvent",
                 "channel":"chatrooms.668.v2","data":"{not-json"}
                """));

    assertEquals("Could not parse Kick WebSocket message", exceptionHandler.message.get());
    assertInstanceOf(Exception.class, exceptionHandler.failure.get());
  }

  @Test
  void reportsMissingRequiredChatFieldsToExceptionHandler() throws Exception {
    KickConsumer consumer = consumer(exchange -> {});
    KickHandler handler = new KickHandler(consumer);
    RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
    consumer.setExceptionHandler(exceptionHandler);

    handler.onChatEvent(
        chatEvent(
            """
                {"id":"message-1","chatroom_id":668,"content":"Hello","type":"message"}
                """,
            true));

    assertEquals("Could not parse Kick WebSocket message", exceptionHandler.message.get());
    assertTrue(
        exceptionHandler
            .failure
            .get()
            .getMessage()
            .contains("Kick chat message sender is required"));
  }

  @Test
  void delegatesProcessorFailuresToExceptionHandler() throws Exception {
    KickConsumer consumer =
        consumer(
            exchange -> {
              throw new IllegalArgumentException("route failed");
            });
    KickHandler handler = new KickHandler(consumer);
    RecordingExceptionHandler exceptionHandler = new RecordingExceptionHandler();
    consumer.setExceptionHandler(exceptionHandler);

    handler.onChatEvent(chatEvent(chatPayload(), true));

    assertEquals("Error processing Kick chat message", exceptionHandler.message.get());
    assertEquals("route failed", exceptionHandler.failure.get().getMessage());
    assertEquals(
        "message-1",
        exceptionHandler.exchange.get().getMessage().getHeader(KickConstants.HEADER_MESSAGE_ID));
  }

  @Test
  void subscribesUsingTheInjectedPusherClient() throws Exception {
    Pusher pusher = new Pusher("test-key", new PusherOptions().setUseTLS(false));
    KickEndpoint endpoint = context.getEndpoint("kick:xqc?chatroomId=668", KickEndpoint.class);
    endpoint.setPusher(pusher);
    KickConsumer consumer = new KickConsumer(endpoint, exchange -> {});

    try {
      consumer.init();

      assertEquals(668L, consumer.getChatroomId());
      assertNotNull(pusher.getChannel("chatrooms.668.v2"));
    } finally {
      consumer.stop();
      pusher.disconnect();
    }
  }

  private KickConsumer consumer(Processor processor) {
    KickEndpoint endpoint = context.getEndpoint("kick:xqc?chatroomId=668", KickEndpoint.class);
    return new KickConsumer(endpoint, processor);
  }

  private static PusherEvent chatEvent(String payload, boolean encodeDataAsText) throws Exception {
    Object data = encodeDataAsText ? payload : OBJECT_MAPPER.readTree(payload);
    String envelope =
        OBJECT_MAPPER.writeValueAsString(
            Map.of(
                "event", CHAT_EVENT,
                "channel", "chatrooms.668.v2",
                "data", data));
    return PusherEvent.fromJson(envelope);
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
                  }
                }
                """;
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
