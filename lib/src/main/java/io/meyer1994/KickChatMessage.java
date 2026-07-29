package io.meyer1994;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KickChatMessage(
        String id,
        @JsonProperty("chatroom_id") Long chatroomId,
        String content,
        String type,
        @JsonProperty("created_at") String createdAt,
        KickChatSender sender) {

    public KickChatMessage {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Kick chat message id is required");
        }
        if (content == null) {
            throw new IllegalArgumentException("Kick chat message content is required");
        }
        if (sender == null) {
            throw new IllegalArgumentException("Kick chat message sender is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Kick chat message type is required");
        }
        if (chatroomId != null && chatroomId <= 0) {
            throw new IllegalArgumentException("Kick chat message chatroom_id must be positive");
        }
    }
}
