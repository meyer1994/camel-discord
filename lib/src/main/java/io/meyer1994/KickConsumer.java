package io.meyer1994;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Consumes Kick chat messages from the public Pusher chatroom channel. */
public class KickConsumer extends DefaultConsumer {
  private static final Logger LOG = LoggerFactory.getLogger(KickConsumer.class);

  private static final String CHAT_MESSAGE_EVENT = "App\\Events\\ChatMessageEvent";

  private final KickEndpoint endpoint;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final HttpClient http = HttpClient.newHttpClient();

  private long chatroomId;

  public KickConsumer(KickEndpoint endpoint, Processor processor) {
    super(endpoint, processor);
    this.endpoint = endpoint;
  }

  @Override
  protected void doInit() throws Exception {
    super.doInit();

    Long configuredChatroomId = endpoint.getChatroomId();
    if (configuredChatroomId != null) {
      chatroomId = configuredChatroomId;
      subscribe(chatroomId);
      return;
    }

    resolveChatroomId(endpoint.getChannel())
        .thenAccept(this::subscribe)
        .exceptionally(
            error -> {
              getExceptionHandler()
                  .handleException(
                      "Could not resolve Kick chatroom for channel " + endpoint.getChannel(),
                      error);
              return null;
            });
  }

  private void subscribe(long id) {
    LOG.info("Resolved chatroom ID for channel {} to {}", endpoint.getChannel(), id);
    endpoint
        .getPusher()
        .subscribe("chatrooms." + id + ".v2")
        .bind(CHAT_MESSAGE_EVENT, new KickHandler(this)::onChatEvent);
  }

  protected CompletableFuture<Long> resolveChatroomId(String channel) throws Exception {
    String str = String.format("https://kick.com/api/v2/channels/%s/chatroom", channel);
    URI uri = URI.create(str);
    LOG.info("Resolving chatroom ID for channel {} to {}", channel, uri);

    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("User-Agent", "xh/0.25.3")
            .GET()
            .build();

    BodyHandler<String> body = HttpResponse.BodyHandlers.ofString();

    return http.sendAsync(request, body)
        .thenApply(
            e -> {
              try {
                chatroomId = objectMapper.readTree(e.body()).get("id").asLong();
                return chatroomId;
              } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
              }
            });
  }

  long getChatroomId() {
    return chatroomId;
  }

  @Override
  public KickEndpoint getEndpoint() {
    return endpoint;
  }
}
