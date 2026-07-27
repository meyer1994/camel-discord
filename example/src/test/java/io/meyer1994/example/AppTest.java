package io.meyer1994.example;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.twitch4j.chat.ITwitchChat;

@SpringBootTest(classes = { App.class, AppTest.TestClientConfiguration.class }, webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class AppTest {

    @LocalServerPort
    private int port;

    @org.springframework.beans.factory.annotation.Autowired
    private CamelContext camelContext;

    @Test
    void servesTheChatPage() {
        assertNotNull(camelContext.getRegistry().lookupByName("chatMessagePublisher"));

        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build()
                .get()
                .uri("/?channel=cellbit")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> {
                    if (!body.contains("Twitch Chat")) {
                        throw new AssertionError("Page does not identify the Twitch chat");
                    }
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClientConfiguration {

        @Bean
        ITwitchChat testTwitchChat() {
            return new TestTwitchChat();
        }
    }
}
