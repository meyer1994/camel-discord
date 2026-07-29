package io.meyer1994;

import org.apache.camel.spi.Metadata;

/** Headers used by the Camel Kick component. */
public interface KickConstants {

    @Metadata(description = "The Kick channel slug.", javaType = "String")
    String HEADER_CHANNEL_NAME = "x-camel-kick-channel-name";

    @Metadata(description = "The Kick chatroom ID.", javaType = "Long")
    String HEADER_CHATROOM_ID = "x-camel-kick-chatroom-id";

    @Metadata(description = "The Kick user ID.", javaType = "Long")
    String HEADER_USER_ID = "x-camel-kick-user-id";

    @Metadata(description = "The Kick display username.", javaType = "String")
    String HEADER_USER_NAME = "x-camel-kick-user-name";

    @Metadata(description = "The Kick chat message ID.", javaType = "String")
    String HEADER_MESSAGE_ID = "x-camel-kick-message-id";

    @Metadata(description = "The Kick event type.", javaType = "String")
    String HEADER_EVENT_TYPE = "x-camel-kick-event-type";
}
