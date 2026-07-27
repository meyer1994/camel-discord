package io.meyer1994.example;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

@Service
public class TwitchStreamService {
    private static final Logger LOG = LoggerFactory.getLogger(TwitchStreamService.class);
    private static final Pattern CHANNEL_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,25}");

    private final CamelContext context;
    private final ChatMessagePublisher publisher;
    private final Scheduler routeScheduler;

    public TwitchStreamService(
            CamelContext context,
            ChatMessagePublisher publisher,
            @Qualifier("routeLifecycleScheduler") Scheduler routeScheduler) {
        this.context = context;
        this.publisher = publisher;
        this.routeScheduler = routeScheduler;
    }

    public Flux<ServerSentEvent<String>> stream(String requestedChannel) {
        String channel = normalizeChannel(requestedChannel);

        return Flux.<ServerSentEvent<String>>create(sink -> {
            String routeId = "twitch-sse-" + channel + "-" + UUID.randomUUID();
            publisher.register(channel, sink);
            try {
                context.addRouteFromTemplate(
                        routeId,
                        TwitchChatRouteTemplate.TEMPLATE_NAME,
                        Map.of("channel", channel));
            } catch (Exception e) {
                publisher.unregister(channel, sink);
                sink.error(e);
                return;
            }

            sink.onDispose(() -> routeScheduler.schedule(() -> {
                publisher.unregister(channel, sink);
                removeRoute(routeId);
            }));
        }).subscribeOn(routeScheduler);
    }

    String normalizeChannel(String channel) {
        if (channel == null || !CHANNEL_PATTERN.matcher(channel.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Twitch channel");
        }
        return channel.trim().toLowerCase(Locale.ROOT);
    }

    private void removeRoute(String routeId) {
        try {
            if (context.getRoute(routeId) != null) {
                context.getRouteController().stopRoute(routeId);
                context.removeRoute(routeId);
            }
        } catch (Exception e) {
            LOG.warn("Unable to remove Twitch SSE route {}", routeId, e);
        }
    }
}
