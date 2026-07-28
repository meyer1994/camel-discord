package io.meyer1994.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;

@Configuration
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
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
