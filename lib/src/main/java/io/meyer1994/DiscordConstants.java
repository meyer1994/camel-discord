package io.meyer1994;

import org.apache.camel.spi.Metadata;

/** Headers used by the Camel Discord component. */
public interface DiscordConstants {
    @Metadata(description = "The Discord event name that produced the exchange.", javaType = "String")
    public static final String HEADER_EVENT = "x-camel-discord-event";
    @Metadata(description = "The Discord message ID.", javaType = "String")
    public static final String HEADER_MESSAGE_ID = "x-camel-discord-message-id";
    @Metadata(description = "The Discord channel ID.", javaType = "String")
    public static final String HEADER_CHANNEL_ID = "x-camel-discord-channel-id";
    @Metadata(description = "The Discord author ID.", javaType = "String")
    public static final String HEADER_AUTHOR_ID = "x-camel-discord-author-id";
    @Metadata(description = "Whether the Discord author is a bot.", javaType = "boolean")
    public static final String HEADER_AUTHOR_IS_BOT = "x-camel-discord-author-is-bot";
    @Metadata(description = "The Discord guild ID, when the event is from a guild.", javaType = "String")
    public static final String HEADER_GUILD_ID = "x-camel-discord-guild-id";
    @Metadata(description = "The Discord channel type.", javaType = "String")
    public static final String HEADER_CHANNEL_TYPE = "x-camel-discord-channel-type";
    @Metadata(description = "Whether the event is from a guild.", javaType = "boolean")
    public static final String HEADER_IS_FROM_GUILD = "x-camel-discord-is-from-guild";
    @Metadata(description = "Whether the event is from a thread.", javaType = "boolean")
    public static final String HEADER_IS_FROM_THREAD = "x-camel-discord-is-from-thread";
    @Metadata(description = "Whether the event is a webhook message.", javaType = "boolean")
    public static final String HEADER_IS_WEBHOOK = "x-camel-discord-is-webhook";
    @Metadata(description = "The Discord message URL.", javaType = "String")
    public static final String HEADER_MESSAGE_URL = "x-camel-discord-message-url";
    @Metadata(description = "The Discord message creation timestamp.", javaType = "java.time.Instant")
    public static final String HEADER_MESSAGE_TIMESTAMP = "x-camel-discord-message-timestamp";
    @Metadata(description = "The IDs of messages in a bulk-delete event.", javaType = "java.util.Set<String>")
    public static final String HEADER_MESSAGE_IDS = "x-camel-discord-message-ids";
    @Metadata(description = "The author ID of the message associated with a reaction event.", javaType = "String")
    public static final String HEADER_MESSAGE_AUTHOR_ID = "x-camel-discord-message-author-id";
    @Metadata(description = "The user ID associated with a reaction event.", javaType = "String")
    public static final String HEADER_REACTION_USER_ID = "x-camel-discord-reaction-user-id";
    @Metadata(description = "The reaction emoji name.", javaType = "String")
    public static final String HEADER_REACTION_EMOJI = "x-camel-discord-reaction-emoji";
}
