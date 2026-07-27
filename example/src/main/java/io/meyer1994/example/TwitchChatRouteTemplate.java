package io.meyer1994.example;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TwitchChatRouteTemplate extends RouteBuilder {
        static final String TEMPLATE_NAME = "twitchChatSse";

        private static final String AI_CATEGORY_SCHEMA = """
                        {
                          "type": "object",
                          "properties": {
                            "categories": {
                              "type": "array",
                              "items": {
                                "type": "string",
                                "enum": [
                                  "TOXIC",
                                  "QUESTION",
                                  "GREETING",
                                  "EMOTE_ONLY",
                                  "HUMOR",
                                  "SPOILER",
                                  "PROMOTION",
                                  "OTHER"
                                ]
                              }
                            }
                          },
                          "required": ["categories"],
                          "additionalProperties": false
                        }
                        """;

        @Override
        public void configure() {
                String insertSql = """
                                INSERT INTO twitch_chat_messages (
                                    channel_id,
                                    channel_name,
                                    user_id,
                                    user_name,
                                    message_id,
                                    message,
                                    raw_event
                                ) VALUES (
                                    :#${header['x-camel-twitch-channel-id']},
                                    :#${header['x-camel-twitch-channel-name']},
                                    :#${header['x-camel-twitch-user-id']},
                                    :#${header['x-camel-twitch-user-name']},
                                    :#${header['x-camel-twitch-message-id']},
                                    :#${body.message},
                                    :#${body.messageEvent.rawMessage}
                                )
                                """;

                String updateCategorySql = """
                                UPDATE twitch_chat_messages
                                SET ai_category = :#${body}
                                WHERE channel_name = :#${header['x-camel-twitch-channel-name']}
                                  AND message_id = :#${header['x-camel-twitch-message-id']}
                                """;

                String updateEmbeddingSql = """
                                UPDATE twitch_chat_messages
                                SET embedding = :#${body}
                                WHERE channel_name = :#${header['x-camel-twitch-channel-name']}
                                  AND message_id = :#${header['x-camel-twitch-message-id']}
                                """;

                routeTemplate(TEMPLATE_NAME)
                                .templateParameter("channel")
                                .from("twitch:{{channel}}?event=CHAT")
                                .log("Twitch chat event: ${header['x-camel-twitch-message-id']}")
                                .setHeader("twitchChatEvent").body()
                                .setBody(header("twitchChatEvent"))
                                .to("sql:" + insertSql + "?noop=true")
                                .setBody(header("twitchChatEvent"))
                                .bean(TwitchStreamService.class, "publish")
                                .wireTap("seda:aiClassification")
                                .wireTap("seda:aiEmbeddings");

                from("seda:aiClassification?concurrentConsumers=8")
                                .setBody(simple("${body.message}"))
                                .log("Classifying message: ${body}")
                                .setHeader("CamelOpenAISystemMessage", constant("""
                                                Classify a Twitch chat message with zero or more categories.
                                                Return only the categories that apply from the allowed enum.
                                                Use OTHER when no specific category applies.
                                                """))
                                .setHeader("CamelOpenAIJsonSchema", constant(AI_CATEGORY_SCHEMA))
                                .to("openai:chat-completion")
                                .log("OpenAI response: ${body}")
                                .to("sql:" + updateCategorySql + "?noop=true");

                from("seda:aiEmbeddings?concurrentConsumers=8")
                                .setBody(simple("${body.message}"))
                                .log("Embedding message: ${body}")
                                .to("openai:embeddings")
                                .setBody(simple("${bodyAs(String)}"))
                                .log("OpenAI embedding generated")
                                .to("sql:" + updateEmbeddingSql + "?noop=true");

                from("sql:SELECT channel_name, count(*) FROM twitch_chat_messages GROUP BY channel_name ORDER BY count(*) DESC LIMIT 10")
                                .log("Channel: ${body}");

                from("direct:emojiCounts")
                                .to("sql:SELECT message FROM twitch_chat_messages WHERE channel_name = :#channel")
                                .bean(EmojiCounter.class, "count");

                from("direct:topChatters")
                                .to("sql:" + """
                                                SELECT user_name, COUNT(*) AS amount
                                                FROM twitch_chat_messages
                                                WHERE channel_name = :#channel
                                                GROUP BY user_name
                                                ORDER BY amount DESC, user_name
                                                LIMIT 10
                                                """);

                from("direct:aiCategoryCounts")
                                .to("sql:" + """
                                                SELECT category, COUNT(*) AS amount
                                                FROM (
                                                    SELECT 'TOXIC' AS category
                                                    FROM twitch_chat_messages
                                                    WHERE channel_name = :#channel AND ai_category LIKE '%"TOXIC"%'
                                                    UNION ALL
                                                    SELECT 'QUESTION'
                                                    FROM twitch_chat_messages
                                                    WHERE channel_name = :#channel AND ai_category LIKE '%"QUESTION"%'
                                                    UNION ALL
                                                    SELECT 'GREETING'
                                                    FROM twitch_chat_messages
                                                    WHERE channel_name = :#channel AND ai_category LIKE '%"GREETING"%'
                                                    UNION ALL
                                                    SELECT 'EMOTE_ONLY'
                                                    FROM twitch_chat_messages
                                                    WHERE channel_name = :#channel AND ai_category LIKE '%"EMOTE_ONLY"%'
                                                    UNION ALL
                                                    SELECT 'HUMOR'
                                                    FROM twitch_chat_messages
                                                    WHERE channel_name = :#channel AND ai_category LIKE '%"HUMOR"%'
                                                    UNION ALL
                                                    SELECT 'SPOILER'
                                                    FROM twitch_chat_messages
                                                    WHERE channel_name = :#channel AND ai_category LIKE '%"SPOILER"%'
                                                    UNION ALL
                                                    SELECT 'PROMOTION'
                                                    FROM twitch_chat_messages
                                                    WHERE channel_name = :#channel AND ai_category LIKE '%"PROMOTION"%'
                                                    UNION ALL
                                                    SELECT 'OTHER'
                                                    FROM twitch_chat_messages
                                                    WHERE channel_name = :#channel AND ai_category LIKE '%"OTHER"%'
                                                ) categories
                                                GROUP BY category
                                                ORDER BY amount DESC
                                                """);

                from("direct:messageStats")
                                .to("sql:" + """
                                                SELECT DATE_TRUNC('MINUTE', created_at) AS bucket,
                                                       COUNT(*) AS amount
                                                FROM twitch_chat_messages
                                                WHERE channel_name = :#channel
                                                  AND created_at >= DATEADD('HOUR', -1, CURRENT_TIMESTAMP())
                                                GROUP BY DATE_TRUNC('MINUTE', created_at)
                                                ORDER BY bucket
                                                """);

                from("direct:chatterStats")
                                .to("sql:" + """
                                                SELECT DATE_TRUNC('MINUTE', created_at) AS bucket,
                                                       COUNT(DISTINCT user_name) AS amount
                                                FROM twitch_chat_messages
                                                WHERE channel_name = :#channel
                                                  AND created_at >= DATEADD('HOUR', -1, CURRENT_TIMESTAMP())
                                                GROUP BY DATE_TRUNC('MINUTE', created_at)
                                                ORDER BY bucket
                                                """);
        }
}
