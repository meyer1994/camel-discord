package io.meyer1994.example;

import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import reactor.core.publisher.Flux;

@Controller
public class Routes {
    private final Twitch stream;
    private final ProducerTemplate producer;
    private final List<String> channels;

    public Routes(
            Twitch stream,
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

    @GetMapping(path = "/chatter", produces = MediaType.TEXT_HTML_VALUE)
    public String chatter(@RequestParam("chatter") String chatter, Model model) {
        model.addAttribute("chatter", chatter);
        return "chatter";
    }

    @ResponseBody
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> events(@RequestParam("channel") String channel) {
        return stream.stream(channel);
    }

    @ResponseBody
    @GetMapping(path = "/events/chatter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatterEvents(@RequestParam("chatter") String chatter) {
        return stream.streamChatter(chatter);
    }

    @ResponseBody
    @GetMapping("/api/stats/messages")
    public List<Map<String, Object>> messages(@RequestParam("channel") String channel) {
        return rows("direct:twitch-chat-messages-5min", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/messages/hour")
    public List<Map<String, Object>> messagesLastHour(@RequestParam("channel") String channel) {
        return rows("direct:twitch-chat-messages-1h", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/messages/total")
    public List<Map<String, Object>> totalMessages(@RequestParam("channel") String channel) {
        return rows("direct:twitch-chat-message-count", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/velocity")
    public List<Map<String, Object>> velocity(@RequestParam("channel") String channel) {
        return rows("direct:twitch-chat-velocity-5min", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/chatters")
    public List<Map<String, Object>> chatters(@RequestParam("channel") String channel) {
        return rows("direct:twitch-chat-chatters-5min", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/chatters/top")
    public List<Map<String, Object>> topChatters(@RequestParam("channel") String channel) {
        return rows("direct:twitch-chat-top-chatters", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/messages/length")
    public List<Map<String, Object>> messageLengths(@RequestParam("channel") String channel) {
        return rows("direct:twitch-chat-message-lengths", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/subscribers/tiers")
    public List<Map<String, Object>> subscriberTiers(@RequestParam("channel") String channel) {
        return rows("direct:twitch-chat-subscription-tiers", channel);
    }

    @ResponseBody
    @GetMapping("/api/stats/activity/hour")
    public List<Map<String, Object>> activityByHour(@RequestParam("channel") String channel) {
        return rows("direct:twitch-chat-activity-by-hour", channel);
    }

    @ResponseBody
    @GetMapping("/api/chatters/summary")
    public List<Map<String, Object>> chatterSummary(@RequestParam("chatter") String chatter) {
        return rows("direct:twitch-chatter-summary", chatter);
    }

    @ResponseBody
    @GetMapping("/api/chatters/channels")
    public List<Map<String, Object>> chatterChannels(@RequestParam("chatter") String chatter) {
        return rows("direct:twitch-chatter-by-channel", chatter);
    }

    @ResponseBody
    @GetMapping("/api/chatters/timeline")
    public List<Map<String, Object>> chatterTimeline(@RequestParam("chatter") String chatter) {
        return rows("direct:twitch-chatter-timeline", chatter);
    }

    @ResponseBody
    @GetMapping("/api/chatters/lengths")
    public List<Map<String, Object>> chatterLengths(@RequestParam("chatter") String chatter) {
        return rows("direct:twitch-chatter-lengths", chatter);
    }

    @ResponseBody
    @GetMapping("/api/chatters/tiers")
    public List<Map<String, Object>> chatterTiers(@RequestParam("chatter") String chatter) {
        return rows("direct:twitch-chatter-tiers", chatter);
    }

    @ResponseBody
    @GetMapping("/api/chatters/activity/hour")
    public List<Map<String, Object>> chatterActivityByHour(@RequestParam("chatter") String chatter) {
        return rows("direct:twitch-chatter-activity-by-hour", chatter);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(String route, String value) {
        return producer.requestBody(route, value, List.class);
    }
}
