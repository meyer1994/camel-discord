package io.meyer1994.example;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("DISCORD_TOKEN must be set before starting the example");
        }

        LOG.info("Starting Discord example");

        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.DIRECT_MESSAGES
                )
                .build()
                .awaitReady();
        LOG.info("Discord client is ready as {}", jda.getSelfUser().getAsTag());

        Main main = new Main(App.class);
        main.bind("client", jda);

        try {
            LOG.info("Starting Camel routes");
            main.run(args);
        } finally {
            LOG.info("Stopping Discord client");
            jda.shutdown();
        }
    }
}
