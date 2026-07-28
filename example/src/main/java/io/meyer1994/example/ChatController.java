package io.meyer1994.example;

import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import reactor.core.publisher.Flux;

@Controller
public class ChatController {
    private final TwitchStreamService stream;
    private final ProducerTemplate producer;
    private final List<String> channels;

    public ChatController(
            TwitchStreamService stream,
            ProducerTemplate producer,
            @Value("${app.twitch.channels}") List<String> channels) {
        this.stream = stream;
        this.producer = producer;
        this.channels = channels;
    }

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index(Model model) {
        model.addAttribute("channels", channels);
        return "index";
    }

    @GetMapping(path = "/channel", produces = MediaType.TEXT_HTML_VALUE)
    public String channel(@RequestParam("channel") String channel, Model model) {
        model.addAttribute("channel", channel);
        return "channel";
    }

    @ResponseBody
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> events(@RequestParam("channel") String channel) throws Exception {
        return stream.stream(channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/messages")
    public Map<String, Object> messages(@RequestParam("channel") String channel) {
        return timeSeries("direct:twitch-chat-messages-5min", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/messages/total")
    public Map<String, Object> totalMessages(@RequestParam("channel") String channel) {
        List<Map<String, Object>> rows = producer.requestBody(
                "direct:twitch-chat-message-count", channel, List.class);
        return Map.of("total", rows.isEmpty() ? 0 : column(rows.getFirst(), "total"));
    }

    @ResponseBody
    @GetMapping("/api/stats/velocity")
    public Map<String, Object> velocity(@RequestParam("channel") String channel) {
        return timeSeries("direct:twitch-chat-velocity-5min", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/chatters")
    public Map<String, Object> chatters(@RequestParam("channel") String channel) {
        return timeSeries("direct:twitch-chat-chatters-5min", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/chatters/top")
    public Map<String, Object> topChatters(@RequestParam("channel") String channel) {
        List<Map<String, Object>> rows = producer.requestBody(
                "direct:twitch-chat-top-chatters", channel, List.class);
        List<Map<String, Object>> items = rows.stream()
                .map(row -> Map.of("user", row.get("username"), "count", row.get("total")))
                .toList();
        return Map.of("items", items);
    }

    @ResponseBody
    @GetMapping("/api/stats/messages/length")
    public Map<String, Object> messageLengths(@RequestParam("channel") String channel) {
        List<Map<String, Object>> rows = producer.requestBody(
                "direct:twitch-chat-message-lengths", channel, List.class);
        List<Map<String, Object>> items = rows.stream()
                .map(row -> Map.of(
                        "bucket", column(row, "bucket"),
                        "count", column(row, "count")))
                .toList();
        return Map.of("items", items);
    }

    @ResponseBody
    @GetMapping("/api/stats/chatters/constellation")
    public Map<String, Object> chatterConstellation(@RequestParam("channel") String channel) {
        List<Map<String, Object>> rows = producer.requestBody(
                "direct:twitch-chat-chatter-constellation", channel, List.class);
        List<Map<String, Object>> items = rows.stream()
                .map(row -> Map.of(
                        "user", column(row, "username"),
                        "messageCount", column(row, "message_count"),
                        "averageMessageLength", column(row, "average_message_length"),
                        "subscriberMonths", column(row, "subscriber_months"),
                        "tier", column(row, "tier")))
                .toList();
        return Map.of("items", items);
    }

    @ResponseBody
    @GetMapping("/api/stats/subscribers/tiers")
    public Map<String, Object> subscriberTiers(@RequestParam("channel") String channel) {
        List<Map<String, Object>> rows = producer.requestBody(
                "direct:twitch-chat-subscription-tiers", channel, List.class);
        List<Map<String, Object>> items = rows.stream()
                .map(row -> Map.of(
                        "tier", column(row, "tier"),
                        "count", column(row, "count")))
                .toList();
        return Map.of("items", items);
    }

    @ResponseBody
    @GetMapping("/api/stats/activity/hour")
    public Map<String, Object> activityByHour(@RequestParam("channel") String channel) {
        List<Map<String, Object>> rows = producer.requestBody(
                "direct:twitch-chat-activity-by-hour", channel, List.class);
        List<Map<String, Object>> items = rows.stream()
                .map(row -> Map.of(
                        "hour", column(row, "hour"),
                        "count", column(row, "count")))
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

    private Object column(Map<String, Object> row, String name) {
        return row.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(0L);
    }
}
