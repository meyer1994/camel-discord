package io.meyer1994;

public enum TwitchEvent {
  CHAT,
  STREAM_ONLINE,
  STREAM_OFFLINE,
  CHANNEL_UPDATE,
  SUBSCRIBE,
  CHEER;

  public boolean isChatEvent() {
    return this == CHAT;
  }
}
