package io.meyer1994.example;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

class ChatControllerTest {
    private CamelContext context;
    private TestTwitchChat client;
    private Scheduler routeScheduler;
    private WebTestClient webClient;

    @BeforeEach
    void setUp() throws Exception {
        context = new DefaultCamelContext();
        client = new TestTwitchChat();
        context.getRegistry().bind("client", client);
        ChatMessagePublisher publisher = new ChatMessagePublisher();
        context.getRegistry().bind("chatMessagePublisher", publisher);
        context.addRoutes(new TwitchChatRouteTemplate());
        context.start();
        routeScheduler = Schedulers.newSingle("test-http-route-lifecycle");
        TwitchStreamService service = new TwitchStreamService(
                context, publisher, routeScheduler);
        webClient = WebTestClient.bindToController(new ChatController(service)).build();
    }

    @AfterEach
    void tearDown() {
        context.stop();
        client.close();
        routeScheduler.dispose();
    }

    @Test
    void rejectsAnInvalidChannelQueryParameter() {
        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/events")
                        .queryParam("channel", "not?a-channel")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }
}
