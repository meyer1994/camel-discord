package io.meyer1994.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(ITwitchClient.class)
    @Profile("!test")
    ITwitchClient twitchClient() {
        return TwitchClientBuilder.builder()
                .withEnableChat(true)
                .withChatAutoJoinOwnChannel(false)
                .build();
    }

}
