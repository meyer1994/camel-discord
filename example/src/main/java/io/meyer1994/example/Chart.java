package io.meyer1994.example;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class Chart {
  @Autowired private TemplateEngine templateEngine;

  // the replay and limit allows us to stream the last 100 points to new
  // subscribers. so they will see a full chart on start
  private final Sinks.Many<Data> sink = Sinks.many().replay().limit(100);

  public Flux<ServerSentEvent<String>> stream() {
    return sink.asFlux()
        .buffer(2, 1)
        .filter(points -> points.size() == 2)
        .map(
            points -> {
              Data prev = points.get(0);
              Data curr = points.get(1);

              Context context = new Context(Locale.ROOT);
              context.setVariable("good", Map.of("start", prev.good_avg, "end", curr.good_avg));
              context.setVariable("bad", Map.of("start", prev.bad_avg, "end", curr.bad_avg));

              String html =
                  templateEngine.process("index", Set.of("chart-fragment"), context).strip();
              return ServerSentEvent.<String>builder(html).build();
            });
  }

  public void publish(Exchange exchange) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> points = exchange.getMessage().getBody(List.class);

    if (points == null) {
      return;
    }

    for (var entry : points) {
      float good = ((Number) entry.get("good_avg")).floatValue();
      float bad = ((Number) entry.get("bad_avg")).floatValue();
      int msg_count = ((Number) entry.get("msg_count")).intValue();
      sink.tryEmitNext(new Data(good, bad, msg_count));
      return; // should only have one either way
    }
  }

  private record Data(float good_avg, float bad_avg, int msg_count) {}
}
