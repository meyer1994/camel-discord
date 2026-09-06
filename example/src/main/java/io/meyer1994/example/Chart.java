package io.meyer1994.example;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import reactor.core.publisher.Flux;

@Service
public class Chart {
    private final TemplateEngine templateEngine;
    private final Topics<String> topics = new Topics<>();

    public Chart(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public Flux<String> stream() {
        return topics.subscribe("chart");
    }

    public void publish(Exchange exchange) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = exchange.getMessage().getBody(List.class);
        if (rows == null || rows.isEmpty()) {
            return;
        }

        Context context = new Context(Locale.ROOT);
        context.setVariable("chartRows", rows);
        String html = templateEngine.process("index", Set.of("chart-fragment"), context).strip();

        topics.publish("chart", html);
    }
}
