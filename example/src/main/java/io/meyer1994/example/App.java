package io.meyer1994.example;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@SpringBootApplication
@EnableCaching
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
    CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("stats", Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .maximumSize(1024)
                .build());
        cacheManager.registerCustomCache("stats-minute", Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(1024)
                .build());
        return cacheManager;
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
