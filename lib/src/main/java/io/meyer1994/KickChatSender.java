package io.meyer1994;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KickChatSender(long id, String username, String slug) {

  public KickChatSender {
    if (id <= 0) {
      throw new IllegalArgumentException("Kick chat sender id must be positive");
    }
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Kick chat sender username is required");
    }
  }
}
