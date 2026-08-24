package io.meyer1994.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class Embeds {
  private static final Logger LOG = LoggerFactory.getLogger(Embeds.class);
  private static final String EMBEDDINGS_ENDPOINT =
      "openai:embeddings?embeddingModel=text-embedding-3-small";

  private static final List<String> GOOD_WORDS = List.of(
      "good", "great", "excellent", "love", "awesome", "funny", "nice");
  private static final List<String> BAD_WORDS = List.of(
      "bad", "terrible", "awful", "hate", "stupid", "boring", "toxic");

  private final ProducerTemplate producerTemplate;
  private final Map<Label, Map<String, List<Float>>> embeddings = new EnumMap<>(Label.class);

  public Embeds(ProducerTemplate producerTemplate) {
    this.producerTemplate = producerTemplate;
    embeddings.put(Label.GOOD, new LinkedHashMap<>());
    embeddings.put(Label.BAD, new LinkedHashMap<>());
  }

  @EventListener(ApplicationReadyEvent.class)
  public void loadReferenceEmbeddings() {
    load(Label.GOOD, GOOD_WORDS);
    load(Label.BAD, BAD_WORDS);
  }

  public Map<String, List<Float>> good() {
    return snapshot(Label.GOOD);
  }

  public Map<String, List<Float>> bad() {
    return snapshot(Label.BAD);
  }

  /**
   * Compares an embedding with the closest good and bad reference embeddings.
   *
   * @return good and bad similarity values from 0 to 100, or {@code null}
   *         until both reference sets have been loaded
   */
  public Similarities similarities(Object value) {
    Map<String, List<Float>> good = snapshot(Label.GOOD);
    Map<String, List<Float>> bad = snapshot(Label.BAD);
    if (good.isEmpty() || bad.isEmpty()) {
      return null;
    }

    List<Float> message = embedding(value);
    double goodSimilarity = toAffinity(maxCosineSimilarity(message, good));
    double badSimilarity = toAffinity(maxCosineSimilarity(message, bad));
    return new Similarities(
        (int) Math.round(goodSimilarity * 100.0),
        (int) Math.round(badSimilarity * 100.0));
  }

  public record Similarities(int good, int bad) {
  }

  private synchronized void load(Label label, List<String> words) {
    try {
      List<?> response = producerTemplate.requestBody(
          EMBEDDINGS_ENDPOINT,
          words,
          List.class);

      if (response.size() != words.size()) {
        throw new IllegalStateException(
            "Expected " + words.size() + " embeddings but received " + response.size());
      }

      Map<String, List<Float>> category = embeddings.get(label);
      category.clear();
      for (int i = 0; i < words.size(); i++) {
        category.put(words.get(i), embedding(response.get(i)));
      }

      LOG.info("Loaded {} {} reference embeddings", response.size(), label.name().toLowerCase());
    } catch (RuntimeException exception) {
      LOG.warn("Could not load {} reference embeddings", label.name().toLowerCase(), exception);
    }
  }

  private static List<Float> embedding(Object value) {
    if (!(value instanceof List<?> vector) || vector.isEmpty()) {
      throw new IllegalStateException("OpenAI returned an invalid embedding vector");
    }

    List<Float> result = new ArrayList<>(vector.size());
    for (Object component : vector) {
      if (!(component instanceof Number number)) {
        throw new IllegalStateException("OpenAI returned an invalid embedding vector");
      }
      result.add(number.floatValue());
    }
    return List.copyOf(result);
  }

  private static double maxCosineSimilarity(
      List<Float> message,
      Map<String, List<Float>> references) {
    double max = -1.0;
    for (List<Float> reference : references.values()) {
      max = Math.max(max, cosineSimilarity(message, reference));
    }
    return max;
  }

  private static double cosineSimilarity(List<Float> left, List<Float> right) {
    if (left.size() != right.size()) {
      throw new IllegalArgumentException("Embedding dimensions do not match");
    }

    double dot = 0.0;
    double leftMagnitude = 0.0;
    double rightMagnitude = 0.0;
    for (int i = 0; i < left.size(); i++) {
      double leftValue = left.get(i);
      double rightValue = right.get(i);
      dot += leftValue * rightValue;
      leftMagnitude += leftValue * leftValue;
      rightMagnitude += rightValue * rightValue;
    }

    if (leftMagnitude == 0.0 || rightMagnitude == 0.0) {
      return 0.0;
    }
    return dot / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
  }

  private static double toAffinity(double cosineSimilarity) {
    // Cosine similarity is [-1, 1]; use a positive value for the relative score.
    return Math.max(0.0, (cosineSimilarity + 1.0) / 2.0);
  }

  private synchronized Map<String, List<Float>> snapshot(Label label) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(embeddings.get(label)));
  }

  private enum Label {
    GOOD,
    BAD
  }
}
