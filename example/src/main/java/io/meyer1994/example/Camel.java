package io.meyer1994.example;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Camel extends RouteBuilder {
  static final String TWITCH_TEMPLATE_NAME = "twitch-chat-listener";
  static final String KICK_TEMPLATE_NAME = "kick-chat-listener";

  @Value("${app.kick.max-messages:100000}")
  private int kickMaxMessages;

  @Value("${app.twitch.max-messages:100000}")
  private int twitchMaxMessages;

  @Value("${app.kick.delete-period:600000}")
  private int kickDeletePeriod;

  @Value("${app.twitch.delete-period:600000}")
  private int twitchDeletePeriod;

  private final Embeds embeds;

  public Camel(Embeds embeds) {
    this.embeds = embeds;
  }

  @Override
  public void configure() {
    routeTemplate(TWITCH_TEMPLATE_NAME)
        .templateParameter("channel")
        .from("twitch:{{channel}}?event=CHAT")
        .log(LoggingLevel.DEBUG, "Received twitch message: ${body}")
        .wireTap("seda:twitch-chat-listener");

    routeTemplate(KICK_TEMPLATE_NAME)
        .templateParameter("channel")
        .from("kick:{{channel}}?event=CHAT")
        .log(LoggingLevel.DEBUG, "Received kick message: ${body}")
        .wireTap("seda:kick-chat-listener");

    from("seda:twitch-chat-insert?concurrentConsumers=32&size=10000")
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
              RETURNING *
            """)
        .split().body()
        .wireTap("seda:twitch-chat-embed")
        .log("Inserted twitch message: ${body[channel_name]} ${body[message_id]}");

    from("seda:kick-chat-insert?concurrentConsumers=32&size=10000")
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
              RETURNING *
            """)
        .split().body()
        .wireTap("seda:kick-chat-embed")
        .log("Inserted kick message: ${body[message_id]} ${body[channel_name]}");

    from("seda:twitch-chat-embed?concurrentConsumers=64&size=10000")
        .setVariable("id").simple("${body[id]}")
        .setVariable("channel_name").simple("${body[channel_name]}")
        .setVariable("message_id").simple("${body[message_id]}")
        .setVariable("message").simple("${body[message]}")
        .log(
            "Getting embedding for twitch message: ${variable:message_id} ${variable:channel_name}")
        .to("openai:embeddings?embeddingModel=text-embedding-3-small")
        .filter().simple("${size()} > 0")
        .process(exchange -> {
          Embeds.Similarities similarities = embeds.similarities(exchange.getMessage().getBody());
          if (similarities != null) {
            exchange.setVariable("goodScore", similarities.good());
            exchange.setVariable("badScore", similarities.bad());
          }
        })
        .setVariable("embedding").simple("${body.toString()}")
        .bean(Twitch.class, "publishScore")
        .to("sql:UPDATE twitch_event_chat SET message_embeddings = :#embedding::vector WHERE id = :#id")
        .log("Updated twitch message: ${variable:message_id} ${variable:channel_name}");

    from("seda:kick-chat-embed?concurrentConsumers=64&size=10000")
        .setVariable("id").simple("${body[id]}")
        .setVariable("channel_name").simple("${body[channel_name]}")
        .setVariable("message_id").simple("${body[message_id]}")
        .setVariable("message").simple("${body[message]}")
        .log(
            "Getting embedding for kick message: ${variable:message_id} ${variable:channel_name}")
        .to("openai:embeddings?embeddingModel=text-embedding-3-small")
        .filter().simple("${size()} > 0")
        .process(exchange -> {
          Embeds.Similarities similarities = embeds.similarities(exchange.getMessage().getBody());
          if (similarities != null) {
            exchange.setVariable("goodScore", similarities.good());
            exchange.setVariable("badScore", similarities.bad());
          }
        })
        .setVariable("embedding").simple("${body.toString()}")
        .bean(Kick.class, "publishScore")
        .to("sql:UPDATE kick_event_chat SET message_embeddings = :#embedding::vector WHERE id = :#id")
        .log("Updated kick message: ${variable:message_id} ${variable:channel_name}");

    from("seda:kick-chat-listener?concurrentConsumers=1&size=10000")
        .bean(Kick.class, "publish")
        .wireTap("seda:kick-chat-insert")
        .log("Published kick message: ${headers['x-camel-kick-channel-name']} ${body.id}");

    from("seda:twitch-chat-listener?concurrentConsumers=1&size=10000")
        .bean(Twitch.class, "publish")
        .wireTap("seda:twitch-chat-insert")
        .log("Published twitch message: ${body.channel.name} ${body.messageEvent.messageId}");

    from(String.format("timer:twitch-chat-delete?period=%d", Math.max(0, twitchDeletePeriod)))
        .setVariable("maxMessages").constant(Math.max(0, this.twitchMaxMessages))
        .to("""
            sql:
              DELETE FROM twitch_event_chat
              WHERE id IN (
                SELECT id
                FROM twitch_event_chat
                ORDER BY event_time DESC NULLS LAST, id DESC
                OFFSET :#${variable.maxMessages}
              )
            """)
        .log("Twitch retention plan: ${body}")
        .log("Deleted beyond the most recent ${variable.maxMessages} twitch messages");

    from(String.format("timer:kick-chat-delete?period=%d", Math.max(0, kickDeletePeriod)))
        .setVariable("maxMessages").constant(Math.max(0, this.kickMaxMessages))
        .to("""
            sql:
              DELETE FROM kick_event_chat
              WHERE id IN (
                SELECT id
                FROM kick_event_chat
                ORDER BY event_time DESC NULLS LAST, id DESC
                OFFSET :#${variable.maxMessages}
              )
            """)
        .log("Kick retention plan: ${body}")
        .log("Deleted beyond the most recent ${variable.maxMessages} kick messages");
  }
}
