package io.meyer1994.example;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public Map<String, Long> emojiCounts() {
        return producerTemplate.requestBody("direct:emojiCounts", null, Map.class);
    }

    @GetMapping(path = "/emoji-chart", produces = MediaType.TEXT_HTML_VALUE)
    public String emojiChart() {
        Map<String, Long> counts = emojiCounts();

        if (counts.isEmpty()) {
            return "<p class=\"text-sm text-slate-400\">No emoji messages yet.</p>";
        }

        long maximum = counts.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(1L);

        StringBuilder html = new StringBuilder();
        html.append("<table class=\"charts-css bar show-labels show-data-axes w-full\">")
                .append("<caption>Emoji usage</caption><tbody>");

        counts.entrySet().stream()
                .limit(8)
                .forEach(entry -> html.append("<tr><th scope=\"row\">")
                        .append(escapeHtml(entry.getKey()))
                        .append("</th><td style=\"--size: ")
                        .append((double) entry.getValue() / maximum)
                        .append("\"><span class=\"data\">")
                        .append(entry.getValue())
                        .append("</span></td></tr>"));

        return html.append("</tbody></table>").toString();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
