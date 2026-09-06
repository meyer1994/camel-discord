package io.meyer1994;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pusher.client.channel.PusherEvent;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;

/** Bridges Kick Pusher chat events into Camel exchanges. */
public class KickHandler {
  private final KickConsumer consumer;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public KickHandler(KickConsumer consumer) {
    this.consumer = consumer;
  }

  public void onChatEvent(PusherEvent event) {
    try {
      emitChatMessage(parseChatMessage(event.getData()));
    } catch (Exception exception) {
      consumer
          .getExceptionHandler()
          .handleException("Could not parse Kick WebSocket message", exception);
    }
  }

  KickChatMessage parseChatMessage(String data) throws Exception {
    if (data == null || data.isBlank()) {
      throw new IllegalArgumentException("Kick chat message data is required");
    }
    return objectMapper.readValue(data, KickChatMessage.class);
  }

  private void emitChatMessage(KickChatMessage event) {
    Exchange exchange = consumer.getEndpoint().createExchange(ExchangePattern.InOnly);
    exchange.getMessage().setBody(event);
    exchange
        .getMessage()
        .setHeader(KickConstants.HEADER_CHANNEL_NAME, consumer.getEndpoint().getChannel());
    exchange
        .getMessage()
        .setHeader(
            KickConstants.HEADER_CHATROOM_ID,
            event.chatroomId() == null ? consumer.getChatroomId() : event.chatroomId());
    exchange.getMessage().setHeader(KickConstants.HEADER_USER_ID, event.sender().id());
    exchange.getMessage().setHeader(KickConstants.HEADER_USER_NAME, event.sender().username());
    exchange.getMessage().setHeader(KickConstants.HEADER_MESSAGE_ID, event.id());
    exchange.getMessage().setHeader(KickConstants.HEADER_EVENT_TYPE, KickEvent.CHAT.name());

    try {
      consumer.getProcessor().process(exchange);
    } catch (Exception exception) {
      consumer
          .getExceptionHandler()
          .handleException("Error processing Kick chat message", exchange, exception);
    }
  }
}
