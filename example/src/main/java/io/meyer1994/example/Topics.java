package io.meyer1994.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class Topics<T> {
  private final Map<String, Sinks.Many<T>> streams = new ConcurrentHashMap<>();

  public Topics() {}

  public Flux<T> subscribe(String channel) {
    return streams
        .computeIfAbsent(channel.toLowerCase(), k -> Sinks.many().multicast().directBestEffort())
        .asFlux();
  }

  public void publish(String channel, T message) {
    streams
        .computeIfAbsent(channel.toLowerCase(), k -> Sinks.many().multicast().directBestEffort())
        .tryEmitNext(message);
  }
}
