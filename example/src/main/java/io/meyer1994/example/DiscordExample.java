package io.meyer1994.example;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import java.util.concurrent.CountDownLatch;

public final class DiscordExample {

    private DiscordExample() {
    }

    public static void main(String[] args) throws Exception {
        String token = requireToken(System.getenv("DISCORD_TOKEN"));
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        DefaultCamelContext context = new DefaultCamelContext();
        context.getRegistry().bind("jda", jda);
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("discord:ping")
                        .filter().simple("${body.contentRaw} == '!ping'")
                        .transform().constant("Pong!")
                        .to("discord:pong");
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(context, jda)));
        context.start();
        new CountDownLatch(1).await();
    }

    static String requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("DISCORD_TOKEN must be set before starting the example");
        }
        return token;
    }

    private static void stop(DefaultCamelContext context, JDA jda) {
        try {
            context.stop();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        jda.shutdown();
    }
}
