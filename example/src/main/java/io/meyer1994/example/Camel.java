package io.meyer1994.example;

import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Camel extends RouteBuilder {
  static final String TEMPLATE_NAME = "twitch-chat-listener";

  @Value("${app.twitch.channels}")
  private List<String> channels;

  @Override
  public void configure() {
    routeTemplate(TEMPLATE_NAME)
        .templateParameter("channel")
        .from("twitch:{{channel}}?event=CHAT")
        .log("Twitch chat event: ${header['x-camel-twitch-message-id']}")
        .bean(Twitch.class, "publish");

    for (String channel : channels) {
      from("twitch:%s?event=CHAT".formatted(channel))
          .wireTap("seda:twitch-chat-insert")
          .wireTap("seda:twitch-chat-listener");
    }

    from("seda:twitch-chat-insert?concurrentConsumers=4")
        .to("""
            sql:
              INSERT INTO twitch_event_chat (
                message_id,
                event_time,
                channel_id,
                channel_name,
                user_id,
                user_name,
                display_name,
                message,
                subscriber_months,
                subscription_tier,
                nonce,
                raw_event
              ) VALUES (
                :#${body.messageEvent.messageId.orElse(null)},
                :#${body.messageEvent.firedAtInstant.atOffset('Z')},
                :#${body.channel.id},
                :#${body.channel.name},
                :#${body.user.id},
                :#${body.user.name},
                :#${body.messageEvent.userDisplayName.orElse(null)},
                :#${body.message},
                :#${body.subscriberMonths},
                :#${body.subscriptionTier},
                :#${body.nonce},
                CAST('{}' AS jsonb)
              )
            """)
        .sample(100)
        .log("Inserted message into database: ${body.channel.name} ${body.messageEvent.messageId}");

    from("seda:twitch-chat-listener")
        .bean(Twitch.class, "publish")
        .sample(100)
        .log("Published message to SEDA: ${body.channel.name} ${body.messageEvent.messageId}");

    from("direct:twitch-chat-top-chatters")
        .to("""
            sql:
              SELECT COALESCE(user_name, 'anonymous') AS username, COUNT(*) AS total
              FROM twitch_event_chat
              WHERE LOWER(TRIM(channel_name)) = LOWER(TRIM(:#${body}))
              GROUP BY user_name
              ORDER BY total DESC
              LIMIT 10
            """);

    from("direct:twitch-chat-messages-5min")
        .to("""
            sql:
              SELECT
                CAST(EXTRACT(EPOCH FROM DATE_TRUNC('minute', event_time)) * 1000 AS BIGINT) AS time,
                COUNT(*) AS value
              FROM twitch_event_chat
              WHERE LOWER(TRIM(channel_name)) = LOWER(TRIM(:#${body}))
                AND event_time >= NOW() - INTERVAL '5 minutes'
              GROUP BY time
              ORDER BY time
            """);

    from("direct:twitch-chat-message-count")
        .to("""
            sql:
              SELECT COUNT(*) AS total
              FROM twitch_event_chat
              WHERE LOWER(TRIM(channel_name)) = LOWER(TRIM(:#${body}))
            """);

    from("direct:twitch-chat-velocity-5min")
        .to("""
            sql:
              WITH messages_per_second AS (
                SELECT
                  DATE_TRUNC('second', event_time) AS second,
                  COUNT(*) AS messages
                FROM twitch_event_chat
                WHERE LOWER(TRIM(channel_name)) = LOWER(TRIM(:#${body}))
                  AND event_time >= NOW() - INTERVAL '5 minutes'
                GROUP BY second
              )
              SELECT
                CAST(EXTRACT(EPOCH FROM second) * 1000 AS BIGINT) AS time,
                ROUND(
                  AVG(messages) OVER (
                    ORDER BY second
                    ROWS BETWEEN 9 PRECEDING AND CURRENT ROW
                  ),
                  2
                ) AS value
              FROM messages_per_second
              ORDER BY second
            """);

    from("direct:twitch-chat-chatters-5min")
        .to("""
            sql:
              SELECT
                CAST(EXTRACT(EPOCH FROM DATE_TRUNC('minute', event_time)) * 1000 AS BIGINT) AS time,
                COUNT(DISTINCT user_id) AS value
              FROM twitch_event_chat
              WHERE LOWER(TRIM(channel_name)) = LOWER(TRIM(:#${body}))
                AND event_time >= NOW() - INTERVAL '5 minutes'
              GROUP BY time
              ORDER BY time
            """);

    from("direct:twitch-chat-message-lengths")
        .to("""
            sql:
              SELECT
                CASE
                  WHEN LENGTH(message) < 20 THEN '0-19'
                  WHEN LENGTH(message) < 50 THEN '20-49'
                  WHEN LENGTH(message) < 100 THEN '50-99'
                  WHEN LENGTH(message) < 200 THEN '100-199'
                  ELSE '200+'
                END AS bucket,
                COUNT(*) AS count
              FROM twitch_event_chat
              WHERE LOWER(TRIM(channel_name)) = LOWER(TRIM(:#${body}))
              GROUP BY bucket
              ORDER BY MIN(LENGTH(message))
            """);

    from("direct:twitch-chat-chatter-constellation")
        .to("""
            sql:
              SELECT
                COALESCE(user_name, 'anonymous') AS username,
                COUNT(*) AS message_count,
                ROUND(AVG(LENGTH(message))::numeric, 2) AS average_message_length,
                MAX(subscriber_months) AS subscriber_months,
                MODE() WITHIN GROUP (ORDER BY subscription_tier) AS tier
              FROM twitch_event_chat
              WHERE LOWER(TRIM(channel_name)) = LOWER(TRIM(:#${body}))
              GROUP BY user_name
              ORDER BY message_count DESC
              LIMIT 100
            """);

    from("direct:twitch-chat-subscription-tiers")
        .to("""
            sql:
              SELECT
                subscription_tier AS tier,
                COUNT(*) AS count
              FROM twitch_event_chat
              WHERE LOWER(TRIM(channel_name)) = LOWER(TRIM(:#${body}))
              GROUP BY subscription_tier
              ORDER BY subscription_tier
            """);

    from("direct:twitch-chat-activity-by-hour")
        .to("""
            sql:
              SELECT
                EXTRACT(HOUR FROM event_time)::INTEGER AS hour,
                COUNT(*) AS count
              FROM twitch_event_chat
              WHERE LOWER(TRIM(channel_name)) = LOWER(TRIM(:#${body}))
              GROUP BY hour
              ORDER BY hour
            """);

    from("direct:twitch-chatter-summary")
        .to("""
            sql:
              SELECT
                COUNT(*) AS messages,
                ROUND(AVG(LENGTH(message))::numeric, 2) AS average_message_length,
                MAX(subscriber_months) AS subscriber_months,
                MODE() WITHIN GROUP (ORDER BY subscription_tier) AS tier
              FROM twitch_event_chat
              WHERE LOWER(TRIM(user_name)) = LOWER(TRIM(:#${body}))
            """);

    from("direct:twitch-chatter-by-channel")
        .to("""
            sql:
              SELECT
                channel_name AS channel,
                COUNT(*) AS messages,
                ROUND(AVG(LENGTH(message))::numeric, 2) AS average_message_length
              FROM twitch_event_chat
              WHERE LOWER(TRIM(user_name)) = LOWER(TRIM(:#${body}))
              GROUP BY channel_name
              ORDER BY messages DESC
            """);

    from("direct:twitch-chatter-timeline")
        .to("""
            sql:
              SELECT
                CAST(EXTRACT(EPOCH FROM DATE_TRUNC('minute', event_time)) * 1000 AS BIGINT) AS time,
                COUNT(*) AS value
              FROM twitch_event_chat
              WHERE LOWER(TRIM(user_name)) = LOWER(TRIM(:#${body}))
              GROUP BY time
              ORDER BY time
            """);

    from("direct:twitch-chatter-lengths")
        .to("""
            sql:
              SELECT
                CASE
                  WHEN LENGTH(message) < 20 THEN '0-19'
                  WHEN LENGTH(message) < 50 THEN '20-49'
                  WHEN LENGTH(message) < 100 THEN '50-99'
                  WHEN LENGTH(message) < 200 THEN '100-199'
                  ELSE '200+'
                END AS bucket,
                COUNT(*) AS count
              FROM twitch_event_chat
              WHERE LOWER(TRIM(user_name)) = LOWER(TRIM(:#${body}))
              GROUP BY bucket
              ORDER BY MIN(LENGTH(message))
            """);

    from("direct:twitch-chatter-tiers")
        .to("""
            sql:
              SELECT subscription_tier AS tier, COUNT(*) AS count
              FROM twitch_event_chat
              WHERE LOWER(TRIM(user_name)) = LOWER(TRIM(:#${body}))
              GROUP BY subscription_tier
              ORDER BY subscription_tier
            """);

    from("direct:twitch-chatter-activity-by-hour")
        .to("""
            sql:
              SELECT
                EXTRACT(HOUR FROM event_time)::INTEGER AS hour,
                COUNT(*) AS count
              FROM twitch_event_chat
              WHERE LOWER(TRIM(user_name)) = LOWER(TRIM(:#${body}))
              GROUP BY hour
              ORDER BY hour
            """);
  }
}
