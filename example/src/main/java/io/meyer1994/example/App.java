package io.meyer1994.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

@Configuration
@SpringBootApplication
public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean(destroyMethod = "disconnect")
    Pusher pusher() {
        Pusher pusher = new Pusher("32cbd69e4b950bf97679", new PusherOptions()
                .setHost("ws-us2.pusher.com")
                .setUseTLS(true)
                .setActivityTimeout(5_000)
                .setPongTimeout(5_000));

        LOG.info("Connecting to Kick WebSocket on ws-us2.pusher.com");

        pusher.connect(
                new ConnectionEventListener() {
                    @Override
                    public void onConnectionStateChange(ConnectionStateChange change) {
                        ConnectionState state = change.getCurrentState();
                        if (state == ConnectionState.CONNECTED)
                            LOG.info("Kick WebSocket connected");
                        if (state == ConnectionState.RECONNECTING)
                            LOG.warn("Kick WebSocket reconnecting");
                        if (state == ConnectionState.DISCONNECTED)
                            LOG.warn("Kick WebSocket disconnected");
                    }

                    @Override
                    public void onError(String message, String code, Exception exception) {
                        String error = String.format("Kick WebSocket error: %s", message);
                        LOG.error(error, exception);
                    }
                });

        return pusher;
    }

    @Bean(destroyMethod = "close")
    ITwitchClient twitchClient() {
        return TwitchClientBuilder.builder()
                .withEnableChat(true)
                // .withEnableHelix(true)
                .withChatAutoJoinOwnChannel(false)
                .build();
    }

    @Bean
    WebFluxConfigurer corsConfigurer() {
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("*");
            }
        };
    }
}
