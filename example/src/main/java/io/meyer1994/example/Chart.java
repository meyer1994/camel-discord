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
  private final Topics<Data> topics = new Topics<>();

  public Chart(TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  public Flux<ServerSentEvent<String>> stream() {
    return topics.subscribe("chart")
        .buffer(2, 1)
        .filter(points -> points.size() == 2)
        .map(points -> {
          Data prev = points.get(0);
          Data curr = points.get(1);

          Context context = new Context(Locale.ROOT);
          context.setVariable("good", Map.of("start", prev.good_avg, "end", curr.good_avg));
          context.setVariable("bad", Map.of("start", prev.bad_avg, "end", curr.bad_avg));

          String html = templateEngine.process("index", Set.of("chart-fragment"), context).strip();
          return ServerSentEvent.<String>builder(html).build();
        });
  }

  public void publish(Exchange exchange) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> points = exchange.getMessage().getBody(List.class);
    if (points == null)
      return;

    for (var entry : points) {
      float good = ((Number) entry.get("good_avg")).floatValue();
      float bad = ((Number) entry.get("bad_avg")).floatValue();
      int msg_count = ((Number) entry.get("msg_count")).intValue();
      Data data = new Data(good, bad, msg_count);
      topics.publish("chart", data);
      return;
    }
  }

  private record Data(float good_avg, float bad_avg, int msg_count) {
  }
}
