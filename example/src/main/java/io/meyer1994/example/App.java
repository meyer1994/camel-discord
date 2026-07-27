package io.meyer1994.example;

import org.apache.camel.main.Main;

import com.github.twitch4j.chat.ITwitchChat;
import com.github.twitch4j.chat.TwitchChatBuilder;

public class App {
    public static void main(String[] args) throws Exception {
        ITwitchChat client = TwitchChatBuilder.builder()
                .withAutoJoinOwnChannel(false)
                .build();

        Main main = new Main(App.class);
        main.bind("client", client);

        try {
            main.run(args);
        } finally {
            client.close();
        }
    }
}
