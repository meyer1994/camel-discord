package io.meyer1994;

import java.util.List;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

import com.github.philippheuer.events4j.api.domain.IDisposable;
import com.github.twitch4j.eventsub.EventSubSubscription;
import com.github.twitch4j.helix.domain.UserList;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

public class TwitchConsumer extends DefaultConsumer {
    private final TwitchEndpoint endpoint;
    private IDisposable subscription;
    private EventSubSubscription eventSubSubscription;

    public TwitchConsumer(TwitchEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        TwitchHandler handler = new TwitchHandler(this);

        if (endpoint.getEvent().isChatEvent()) {
            subscription = endpoint.getClient().getEventManager()
                    .onEvent(ChannelMessageEvent.class,
                            handler::onChannelMessage);
            endpoint.getClient().getChat().joinChannel(endpoint.getChannel());
            return;
        }

        String broadcasterUserId = resolveBroadcasterUserId();
        subscription = endpoint.getClient().getEventSocket().getEventManager()
                .onEvent(endpoint.getEventClass(), handler::onEvent);
        eventSubSubscription = endpoint.createEventSubSubscription(broadcasterUserId);
        endpoint.getClient().getEventSocket().register(eventSubSubscription);
    }

    @Override
    protected void doStop() throws Exception {
        if (subscription != null) {
            subscription.dispose();
            subscription = null;
        }

        if (endpoint.getEvent().isChatEvent()) {
            endpoint.getClient().getChat().leaveChannel(endpoint.getChannel());
        } else if (eventSubSubscription != null) {
            endpoint.getClient().getEventSocket().unregister(eventSubSubscription);
            eventSubSubscription = null;
        }
        super.doStop();
    }

    private String resolveBroadcasterUserId() {
        UserList users = endpoint.getClient().getHelix()
                .getUsers(null, List.of(endpoint.getChannel()), null)
                .execute();
        return users.getUsers().getFirst().getId();
    }

    @Override
    public TwitchEndpoint getEndpoint() {
        return endpoint;
    }
}
