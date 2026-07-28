package io.meyer1994.example;

import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwitchChatRouteTemplate extends RouteBuilder {
  static final String TEMPLATE_NAME = "twitch-chat-listener";

  @Value("${app.twitch.channels}")
  private List<String> channels;

  @Override
  public void configure() {
    routeTemplate(TEMPLATE_NAME)
        .templateParameter("channel")
        .from("twitch:{{channel}}?event=CHAT")
        .log("Twitch chat event: ${header['x-camel-twitch-message-id']}")
        .bean(TwitchStreamService.class, "publish");

    for (String channel : channels) {
      from("twitch:%s?event=STREAM_ONLINE".formatted(channel))
          .log("Stream online: ${body.type}:${body.id}");
      from("twitch:%s?event=STREAM_OFFLINE".formatted(channel))
          .log("Stream offline: ${body.type}:${body.id}");
    }

    from("twitch:aspaszin?event=CHAT")
        .log("Inserting message into database: ${body.messageEvent.messageId}")
        .to("""
            sql:INSERT INTO twitch_event_chat (
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
        .to("seda:twitch-chat-listener");

    from("seda:twitch-chat-listener")
        .log("Published message to SEDA: ${body.messageEvent.messageId}");
  }
}
