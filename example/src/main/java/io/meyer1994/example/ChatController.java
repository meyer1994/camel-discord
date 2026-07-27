package io.meyer1994.example;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final TwitchStreamService streamService;
    private final ProducerTemplate producerTemplate;

    public ChatController(TwitchStreamService streamService, ProducerTemplate producerTemplate) {
        this.streamService = streamService;
        this.producerTemplate = producerTemplate;
    }

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String page(@RequestParam("channel") String channel) throws IOException {
        return new ClassPathResource("static/index.html")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("__CHANNEL__", URLEncoder.encode(channel, StandardCharsets.UTF_8));
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> events(@RequestParam("channel") String channel) throws Exception {
        return streamService.stream(channel);
    }

    @GetMapping(path = "/emoji-counts", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("unchecked")
    public Map<String, Long> emojiCounts(@RequestParam("channel") String channel) {
        return producerTemplate.requestBodyAndHeader(
                "direct:emojiCounts", null, "channel", channel, Map.class);
    }

    @GetMapping(path = "/api/stats/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> messageStats(@RequestParam("channel") String channel) {
        return Map.of("points", timeSeries("direct:messageStats", channel));
    }

    @GetMapping(path = "/api/stats/chatters", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> chatterStats(@RequestParam("channel") String channel) {
        return Map.of("points", timeSeries("direct:chatterStats", channel));
    }

    @GetMapping(path = "/api/stats/emojis", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> emojiStats(@RequestParam("channel") String channel) {
        return Map.of("items", emojiCounts(channel).entrySet().stream()
                .limit(8)
                .map(entry -> Map.of("emoji", entry.getKey(), "count", entry.getValue()))
                .toList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> timeSeries(String route, String channel) {
        List<Map<String, Object>> rows = producerTemplate.requestBodyAndHeader(
                route, null, "channel", channel, List.class);
        Map<LocalDateTime, Long> values = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Timestamp bucket = (Timestamp) row.get("BUCKET");
            Number amount = (Number) row.get("AMOUNT");
            values.put(bucket.toLocalDateTime(), amount.longValue());
        }

        LocalDateTime end = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime start = end.minusMinutes(59);
        List<Map<String, Object>> points = new ArrayList<>();

        for (int minute = 0; minute < 60; minute++) {
            LocalDateTime bucket = start.plusMinutes(minute);
            points.add(Map.of(
                    "time", bucket.toString(),
                    "value", values.getOrDefault(bucket, 0L)));
        }

        return points;
    }
}
