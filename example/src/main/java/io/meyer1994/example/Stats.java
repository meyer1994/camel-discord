package io.meyer1994.example;

import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class Stats {
    private final ProducerTemplate producer;

    public Stats(ProducerTemplate producer) {
        this.producer = producer;
    }

    @Cacheable(cacheNames = "stats", key = "{#route, #value}", sync = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> rows(String route, String value) {
        return producer.requestBody(route, value, List.class);
    }
}
