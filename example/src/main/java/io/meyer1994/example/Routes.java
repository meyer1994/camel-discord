package io.meyer1994.example;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import reactor.core.publisher.Flux;

@Controller
public class Routes {
    @Autowired
    private Twitch twitch;

    @Autowired
    private Kick kick;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private Chart chart;

    @Value("${app.twitch.channels}")
    private Set<String> channels = Collections.emptySet();

    private final Set<String> configuredChannels = ConcurrentHashMap.newKeySet();

    private static final Logger logger = LoggerFactory.getLogger(Routes.class);

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index(Model model) {
        model.addAttribute("channels", channels);
        model.addAttribute("feed", false);
        return "index";
    }

    @GetMapping(path = "/c", produces = MediaType.TEXT_HTML_VALUE)
    public String selectChannel(@RequestParam("channel") String channel) {
        return "redirect:/c/" + UriUtils.encodePathSegment(channel, java.nio.charset.StandardCharsets.UTF_8);
    }

    @GetMapping(path = "/c/{channel}", produces = MediaType.TEXT_HTML_VALUE)
    public String channel(@PathVariable("channel") String channel, Model model) throws Exception {
        channel = channel.trim().toLowerCase();

        synchronized (configuredChannels) {
            String kickEndpoint = String.format("kick:channel:%s?event=CHAT", channel);
            String twitchEndpoint = String.format("twitch:channel:%s?event=CHAT", channel);

            if (camelContext.hasEndpoint(kickEndpoint) == null) {
                logger.info("Adding KICK route for channel: {}", channel);
                camelContext.addRouteFromTemplate(kickEndpoint,
                        Camel.KICK_TEMPLATE_NAME,
                        Map.of("channel", channel));
            }

            if (camelContext.hasEndpoint(twitchEndpoint) == null) {
                logger.info("Adding TWITCH route for channel: {}", channel);
                camelContext.addRouteFromTemplate(twitchEndpoint,
                        Camel.TWITCH_TEMPLATE_NAME,
                        Map.of("channel", channel));
            }

            configuredChannels.add(channel);
        }

        model.addAttribute("feed", true);
        model.addAttribute("channel", channel);
        return "index";
    }

    @ResponseBody
    @GetMapping(path = "/c/{channel}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> events(@PathVariable("channel") String channel) {
        channel = channel.trim().toLowerCase();

        if (!configuredChannels.contains(channel)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel is not configured: " + channel);
        }

        return withHeartbeat(Flux.merge(
                kick.stream(channel),
                twitch.stream(channel)));
    }

    @ResponseBody
    @GetMapping(path = "/chart/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chartData() {
        return withHeartbeat(
            chart.stream()
                .map(html -> ServerSentEvent.<String>builder(html).build()));
    }

    private static Flux<ServerSentEvent<String>> withHeartbeat(
            Flux<ServerSentEvent<String>> events) {
        Flux<ServerSentEvent<String>> heartbeats = Flux.interval(Duration.ofSeconds(15))
                .map(tick -> ServerSentEvent.<String>builder().comment("keepalive").build());
        return Flux.merge(events, heartbeats);
    }
}
