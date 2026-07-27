package io.meyer1994;

import org.apache.camel.spi.Metadata;

/** Headers used by the Camel Twitch component. */
public interface TwitchConstants {

    @Metadata(description = "The Twitch channel ID.", javaType = "String")
    String HEADER_CHANNEL_ID = "x-camel-twitch-channel-id";

    @Metadata(description = "The Twitch channel login.", javaType = "String")
    String HEADER_CHANNEL_NAME = "x-camel-twitch-channel-name";

    @Metadata(description = "The Twitch user ID.", javaType = "String")
    String HEADER_USER_ID = "x-camel-twitch-user-id";

    @Metadata(description = "The Twitch user login.", javaType = "String")
    String HEADER_USER_NAME = "x-camel-twitch-user-name";

    @Metadata(description = "The Twitch chat message ID.", javaType = "String")
    String HEADER_MESSAGE_ID = "x-camel-twitch-message-id";

    @Metadata(description = "The Twitch EventSub event type.", javaType = "String")
    String HEADER_EVENT_TYPE = "x-camel-twitch-event-type";

    @Metadata(description = "The Twitch event timestamp.", javaType = "java.time.Instant")
    String HEADER_EVENT_TIMESTAMP = "x-camel-twitch-event-timestamp";
}
