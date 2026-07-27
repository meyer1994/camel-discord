package io.meyer1994.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.github.twitch4j.chat.ITwitchChat;
import com.github.twitch4j.chat.TwitchChatBuilder;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(ITwitchChat.class)
    @Profile("!test")
    ITwitchChat twitchChat() {
        return TwitchChatBuilder.builder()
                .withAutoJoinOwnChannel(false)
                .build();
    }

}
