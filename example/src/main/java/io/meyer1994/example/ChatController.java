package io.meyer1994.example;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
public class ChatController {
    private final TwitchStreamService stream;
    private final ProducerTemplate producer;

    public ChatController(TwitchStreamService stream, ProducerTemplate producer) {
        this.stream = stream;
        this.producer = producer;
    }

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String page(@RequestParam("channel") String channel) throws IOException {
        return new ClassPathResource("static/index.html")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("__CHANNEL__", URLEncoder.encode(channel, StandardCharsets.UTF_8));
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> events(@RequestParam("channel") String channel) throws Exception {
        return stream.stream(channel);
    }

    @GetMapping("/api/stats/messages")
    public Map<String, Object> messages(@RequestParam("channel") String channel) {
        return timeSeries("direct:twitch-chat-messages-5min", channel);
    }

    @GetMapping("/api/stats/chatters")
    public Map<String, Object> chatters(@RequestParam("channel") String channel) {
        return timeSeries("direct:twitch-chat-chatters-5min", channel);
    }

    @GetMapping("/api/stats/chatters/top")
    public Map<String, Object> topChatters(@RequestParam("channel") String channel) {
        List<Map<String, Object>> rows = producer.requestBody(
                "direct:twitch-chat-top-chatters", channel, List.class);
        List<Map<String, Object>> items = rows.stream()
                .map(row -> Map.of("user", row.get("username"), "count", row.get("total")))
                .toList();
        return Map.of("items", items);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> timeSeries(String route, String channel) {
        List<Map<String, Object>> rows = producer.requestBody(route, channel, List.class);
        List<Map<String, Object>> points = rows.stream()
                .map(row -> Map.of("time", row.get("time"), "value", row.get("value")))
                .toList();
        return Map.of("points", points);
    }
}
