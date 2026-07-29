package io.meyer1994;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pusher.client.channel.Channel;

/** Consumes Kick chat messages from the public Pusher chatroom channel. */
public class KickConsumer extends DefaultConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(KickConsumer.class);

    private static final String CHAT_MESSAGE_EVENT = "App\\Events\\ChatMessageEvent";

    private final KickEndpoint endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    private long chatroomId;

    public KickConsumer(KickEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    protected void doInit() throws Exception {
        chatroomId = endpoint.getChatroomId() == null
                ? resolveChatroomId(endpoint.getChannel())
                : endpoint.getChatroomId();
        LOG.info("Resolved chatroom ID for channel {} to {}", endpoint.getChannel(), chatroomId);

        Channel subscribedChannel = endpoint.getPusher().subscribe("chatrooms." + chatroomId + ".v2");
        KickHandler handler = new KickHandler(this);
        subscribedChannel.bind(CHAT_MESSAGE_EVENT, handler::onChatEvent);

        super.doInit();
    }

    protected long resolveChatroomId(String channel) throws Exception {
        String str = String.format("https://kick.com/api/v2/channels/%s/chatroom", channel);
        URI uri = URI.create(str);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .GET()
                .build();

        BodyHandler<String> body = HttpResponse.BodyHandlers.ofString();
        HttpResponse<String> response = http.send(request, body);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Kick chatroom lookup returned HTTP " + response.statusCode()
                            + "; configure chatroomId explicitly if Kick blocks anonymous lookup");
        }

        JsonNode node = objectMapper.readTree(response.body()).get("id");
        return node.asLong();
    }

    long getChatroomId() {
        return chatroomId;
    }

    @Override
    public KickEndpoint getEndpoint() {
        return endpoint;
    }
}
