package io.meyer1994;

import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.eventsub.events.ChannelCheerEvent;
import com.github.twitch4j.eventsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.eventsub.events.ChannelUpdateV2Event;
import com.github.twitch4j.eventsub.events.StreamOfflineEvent;
import com.github.twitch4j.eventsub.events.StreamOnlineEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import java.util.Locale;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/** Receive chat or EventSub events from a Twitch channel. */
@UriEndpoint(
    firstVersion = "0.0.2",
    scheme = "twitch",
    title = "Twitch",
    syntax = "twitch:channel",
    category = Category.SOCIAL,
    consumerOnly = true,
    headersClass = TwitchConstants.class)
public class TwitchEndpoint extends DefaultEndpoint {

  @UriParam
  @Metadata(autowired = true)
  private ITwitchClient client;

  @UriPath(description = "Twitch channel login")
  @Metadata(required = true)
  private String channel;

  @UriParam(
      description =
          "Event to consume: CHAT, STREAM_ONLINE, STREAM_OFFLINE, CHANNEL_UPDATE, SUBSCRIBE, or CHEER")
  @Metadata(required = true)
  private TwitchEvent event;

  public TwitchEndpoint() {}

  public TwitchEndpoint(String endpointUri, TwitchComponent component) {
    super(endpointUri, component);
  }

  @Override
  public Producer createProducer() {
    throw new UnsupportedOperationException("The Twitch component is consumer-only");
  }

  @Override
  public Consumer createConsumer(Processor processor) throws Exception {
    TwitchConsumer consumer = new TwitchConsumer(this, processor);
    this.configureConsumer(consumer);
    return consumer;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel.trim().toLowerCase(Locale.ROOT);
  }

  public ITwitchClient getClient() {
    return client;
  }

  /** The externally managed Twitch4J client. */
  public void setClient(ITwitchClient client) {
    this.client = client;
  }

  public TwitchEvent getEvent() {
    return event;
  }

  public void setEvent(TwitchEvent event) {
    this.event = event;
  }

  public Class<?> getEventClass() {
    return switch (event) {
      case STREAM_ONLINE -> StreamOnlineEvent.class;
      case STREAM_OFFLINE -> StreamOfflineEvent.class;
      case CHANNEL_UPDATE -> ChannelUpdateV2Event.class;
      case SUBSCRIBE -> ChannelSubscribeEvent.class;
      case CHEER -> ChannelCheerEvent.class;
      default -> throw new IllegalArgumentException("Unsupported Twitch event: " + event);
    };
  }

  public EventSubSubscription createEventSubSubscription(String broadcasterUserId) {
    return switch (event) {
      case STREAM_ONLINE ->
          SubscriptionTypes.STREAM_ONLINE.prepareSubscription(
              condition -> condition.broadcasterUserId(broadcasterUserId).build(), null);
      case STREAM_OFFLINE ->
          SubscriptionTypes.STREAM_OFFLINE.prepareSubscription(
              condition -> condition.broadcasterUserId(broadcasterUserId).build(), null);
      case CHANNEL_UPDATE ->
          SubscriptionTypes.CHANNEL_UPDATE_V2.prepareSubscription(
              condition -> condition.broadcasterUserId(broadcasterUserId).build(), null);
      case SUBSCRIBE ->
          SubscriptionTypes.CHANNEL_SUBSCRIBE.prepareSubscription(
              condition -> condition.broadcasterUserId(broadcasterUserId).build(), null);
      case CHEER ->
          SubscriptionTypes.CHANNEL_CHEER.prepareSubscription(
              condition -> condition.broadcasterUserId(broadcasterUserId).build(), null);
      default -> throw new IllegalArgumentException("Unsupported Twitch event: " + event);
    };
  }
}
