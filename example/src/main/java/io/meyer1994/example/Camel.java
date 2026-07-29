package io.meyer1994.example;

import java.util.Set;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Camel extends RouteBuilder {
  static final String TEMPLATE_NAME = "twitch-chat-listener";

  @Value("${app.twitch.channels}")
  private Set<String> channels;

  @Value("${app.kick.channels}")
  private Set<String> kickChannels;

  private static final int SAMPLE = 100;

  @Override
  public void configure() {
    routeTemplate(TEMPLATE_NAME)
        .templateParameter("channel")
        .from("twitch:{{channel}}?event=CHAT")
        .log("Twitch chat event: ${header['x-camel-twitch-message-id']}")
        .bean(Twitch.class, "publish");

    for (String channel : channels) {
      from("twitch:%s?event=CHAT".formatted(channel))
          // .log("TWITCH CHAT EVENT: ${body}")
          .wireTap("seda:twitch-chat-insert")
          .wireTap("seda:twitch-chat-listener");
    }

    for (String channel : kickChannels) {
      from("kick:%s?event=CHAT".formatted(channel))
          // .log("KICK CHAT EVENT: ${body}")
          .wireTap("seda:kick-chat-insert")
          .wireTap("seda:kick-chat-listener");
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
                jsonb_build_object(
                  'eventType', 'ChannelMessageEvent',
                  'channel', jsonb_build_object(
                    'id', CAST(:#${body.channel.id} AS TEXT),
                    'name', CAST(:#${body.channel.name} AS TEXT)
                  ),
                  'user', jsonb_build_object(
                    'id', CAST(:#${body.user.id} AS TEXT),
                    'name', CAST(:#${body.user.name} AS TEXT)
                  ),
                  'message', CAST(:#${body.message} AS TEXT),
                  'subscriberMonths', CAST(:#${body.subscriberMonths} AS INTEGER),
                  'subscriptionTier', CAST(:#${body.subscriptionTier} AS INTEGER),
                  'nonce', CAST(:#${body.nonce} AS TEXT),
                  'messageEvent', jsonb_build_object(
                    'messageId', CAST(:#${body.messageEvent.messageId.orElse(null)} AS TEXT),
                    'firedAt', CAST(:#${body.messageEvent.firedAtInstant.atOffset('Z')} AS TEXT),
                    'rawMessage', CAST(:#${body.messageEvent.rawMessage} AS TEXT),
                    'commandType', CAST(:#${body.messageEvent.commandType} AS TEXT),
                    'channelId', CAST(:#${body.messageEvent.channelId} AS TEXT),
                    'channelName', CAST(:#${body.messageEvent.channelName.orElse(null)} AS TEXT),
                    'message', CAST(:#${body.messageEvent.message.orElse(null)} AS TEXT),
                    'payload', CAST(:#${body.messageEvent.payload.orElse(null)} AS TEXT),
                    'clientName', CAST(:#${body.messageEvent.clientName.orElse(null)} AS TEXT),
                    'userId', CAST(:#${body.messageEvent.userId} AS TEXT),
                    'userName', CAST(:#${body.messageEvent.userName} AS TEXT),
                    'userDisplayName', CAST(:#${body.messageEvent.userDisplayName.orElse(null)} AS TEXT),
                    'userChatColor', CAST(:#${body.messageEvent.userChatColor.orElse(null)} AS TEXT),
                    'targetUserId', CAST(:#${body.messageEvent.targetUserId} AS TEXT),
                    'nonce', CAST(:#${body.messageEvent.nonce.orElse(null)} AS TEXT),
                    'subscriberMonths', CAST(:#${body.messageEvent.subscriberMonths.orElse(0)} AS INTEGER),
                    'subscriptionTier', CAST(:#${body.messageEvent.subscriptionTier.orElse(0)} AS INTEGER)
                  ),
                  'replyInfo', NULL,
                  'chantInfo', NULL,
                  'botOwnerIds', jsonb_build_array()
                )::JSONB
              )
            """)
        .sample(SAMPLE)
        .log(
            "Inserted twitch message: ${body.channel.name} ${body.messageEvent.messageId} (sample: ${headers['x-camel-sample-count']})");

    from("seda:kick-chat-insert?concurrentConsumers=4")
        .to("""
            sql:
              INSERT INTO kick_event_chat (
                message_id,
                event_time,
                chatroom_id,
                channel_name,
                user_id,
                user_name,
                message,
                message_type,
                raw_event
              ) VALUES (
                :#${body.id},
                CAST(:#${body.createdAt} AS TIMESTAMPTZ),
                :#${body.chatroomId},
                :#${headers['x-camel-kick-channel-name']},
                :#${body.sender.id},
                :#${body.sender.username},
                :#${body.content},
                :#${body.type},
                CAST('{}' AS jsonb)
              )
            """)
        .sample(SAMPLE)
        .log(
            "Inserted kick message: ${headers['x-camel-kick-channel-name']} ${body.id} (sample: ${headers['x-camel-sample-count']})");

    from("seda:kick-chat-listener")
        .bean(Kick.class, "publish")
        .sample(SAMPLE)
        .log("Published kick message to SEDA: ${headers['x-camel-kick-channel-name']} ${body.id}");

    from("seda:twitch-chat-listener")
        .bean(Twitch.class, "publish")
        .sample(SAMPLE)
        .log("Published twitch message to SEDA: ${body.channel.name} ${body.messageEvent.messageId}");

    from("direct:twitch-chat-top-chatters")
        .to("""
            sql:
              SELECT COALESCE(user_name, 'anonymous') AS username, COUNT(*) AS total
              FROM twitch_event_chat
              WHERE channel_name = :#${body}
              GROUP BY user_name
              ORDER BY total DESC
              LIMIT 10
            """);

    from("direct:twitch-chat-messages-5min")
        .to("""
            sql:
              SELECT
                CAST(EXTRACT(EPOCH FROM DATE_TRUNC('second', event_time)) * 100 AS BIGINT) AS time,
                COUNT(*) AS value
              FROM twitch_event_chat
              WHERE channel_name = :#${body}
                AND event_time >= NOW() - INTERVAL '5 minutes'
              GROUP BY time
              ORDER BY time
            """);

    from("direct:twitch-chat-messages-1h")
        .to("""
            sql:
              SELECT
                CAST(EXTRACT(EPOCH FROM DATE_TRUNC('minute', event_time)) * 100 AS BIGINT) AS time,
                COUNT(*) AS value
              FROM twitch_event_chat
              WHERE channel_name = :#${body}
                AND event_time >= NOW() - INTERVAL '1 hour'
              GROUP BY time
              ORDER BY time
            """);

    from("direct:twitch-chat-message-count")
        .to("""
            sql:
              SELECT COUNT(*) AS total
              FROM twitch_event_chat
              WHERE channel_name = :#${body}
            """);

    from("direct:twitch-chat-velocity-5min")
        .to("""
            sql:
              WITH messages_per_second AS (
                SELECT
                  DATE_TRUNC('second', event_time) AS second,
                  COUNT(*) AS messages
                FROM twitch_event_chat
                WHERE channel_name = :#${body}
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
                CAST(EXTRACT(EPOCH FROM DATE_TRUNC('second', event_time)) * 1000 AS BIGINT) AS time,
                COUNT(DISTINCT user_id) AS value
              FROM twitch_event_chat
              WHERE channel_name = :#${body}
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
                END AS label,
                COUNT(*) AS value
              FROM twitch_event_chat
              WHERE channel_name = :#${body}
              GROUP BY label
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
              WHERE channel_name = :#${body}
              GROUP BY user_name
              ORDER BY message_count DESC
              LIMIT 100
            """);

    from("direct:twitch-chat-subscription-tiers")
        .to("""
            sql:
              SELECT
                CASE COALESCE(subscription_tier, 0)
                  WHEN 0 THEN 'Non-subscriber'
                  WHEN 1 THEN 'Tier 1'
                  WHEN 2 THEN 'Tier 2'
                  WHEN 3 THEN 'Tier 3'
                  ELSE 'Tier ' || COALESCE(subscription_tier, 0)::TEXT
                END AS label,
                COUNT(*) AS value
              FROM twitch_event_chat
              WHERE channel_name = :#${body}
              GROUP BY COALESCE(subscription_tier, 0)
              ORDER BY COALESCE(subscription_tier, 0)
            """);

    from("direct:twitch-chat-activity-by-hour")
        .to("""
            sql:
              SELECT
                EXTRACT(HOUR FROM event_time)::INTEGER AS hour,
                COUNT(*) AS count
              FROM twitch_event_chat
              WHERE channel_name = :#${body}
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
              WHERE user_name = :#${body}
            """);

    from("direct:twitch-chatter-by-channel")
        .to("""
            sql:
              SELECT
                channel_name AS channel,
                COUNT(*) AS messages,
                ROUND(AVG(LENGTH(message))::numeric, 2) AS average_message_length
              FROM twitch_event_chat
              WHERE user_name = :#${body}
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
              WHERE user_name = :#${body}
                AND event_time >= NOW() - INTERVAL '5 minutes'
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
                END AS label,
                COUNT(*) AS value
              FROM twitch_event_chat
              WHERE user_name = :#${body}
              GROUP BY label
              ORDER BY MIN(LENGTH(message))
            """);

    from("direct:twitch-chatter-tiers")
        .to("""
            sql:
              SELECT
                CASE COALESCE(subscription_tier, 0)
                  WHEN 0 THEN 'Non-subscriber'
                  WHEN 1 THEN 'Tier 1'
                  WHEN 2 THEN 'Tier 2'
                  WHEN 3 THEN 'Tier 3'
                  ELSE 'Tier ' || COALESCE(subscription_tier, 0)::TEXT
                END AS label,
                COUNT(*) AS value
              FROM twitch_event_chat
              WHERE user_name = :#${body}
              GROUP BY COALESCE(subscription_tier, 0)
              ORDER BY COALESCE(subscription_tier, 0)
            """);

    from("direct:twitch-chatter-activity-by-hour")
        .to("""
            sql:
              SELECT
                EXTRACT(HOUR FROM event_time)::INTEGER AS hour,
                COUNT(*) AS count
              FROM twitch_event_chat
              WHERE user_name = :#${body}
              GROUP BY hour
              ORDER BY hour
            """);
  }
}
