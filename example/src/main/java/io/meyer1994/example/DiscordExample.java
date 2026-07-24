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
        DefaultCamelContext context = new DefaultCamelContext();
        
        JDA jda = JDABuilder.createDefault(System.getenv("DISCORD_TOKEN"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        context.getRegistry().bind("jda", jda);

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("discord:ping")
                        .log("Message received: ${body.contentRaw}")
                        .filter().simple("${body.contentRaw} == '!ping'")
                        .transform().constant("Pong!")
                        .to("discord:pong");
                
                from("discord:nice")
                        .log("Message received: ${body.contentRaw}")
                        .filter().simple("${body.contentRaw} == '!nice'")
                        .split(constant("🇳,🇮,🇨,🇪"))
                                .delimiter(",")
                                .to("discord:nice-reaction?operation=MESSAGE_REACT")
                        .end();
            }
        });

        Runtime.getRuntime()
            .addShutdownHook(new Thread(() -> stop(context, jda)));

        context.start();
        new CountDownLatch(1).await();
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
