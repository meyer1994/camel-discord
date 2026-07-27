package io.meyer1994.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

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

    public ChatController(TwitchStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String page(@RequestParam("channel") String channel) throws IOException {
        String normalizedChannel = streamService.normalizeChannel(channel);
        return new ClassPathResource("static/index.html")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("__CHANNEL__", URLEncoder.encode(normalizedChannel, StandardCharsets.UTF_8));
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> events(@RequestParam("channel") String channel) {
        return streamService.stream(channel);
    }
}
