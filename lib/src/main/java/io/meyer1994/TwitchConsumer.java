package io.meyer1994;

import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

import com.github.philippheuer.events4j.api.domain.IEventSubscription;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

public class TwitchConsumer extends DefaultConsumer {
    private final TwitchEndpoint endpoint;
    private IEventSubscription subscription;

    public TwitchConsumer(TwitchEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        TwitchHandler handler = new TwitchHandler(this);
        subscription = endpoint.getClient().getEventManager()
                .onEvent(ChannelMessageEvent.class, handler::onChannelMessage);
        endpoint.getClient().joinChannel(endpoint.getChannel());
    }

    @Override
    protected void doStop() throws Exception {
        subscription.dispose();
        subscription = null;
        endpoint.getClient().leaveChannel(endpoint.getChannel());
        super.doStop();
    }

    @Override
    public TwitchEndpoint getEndpoint() {
        return endpoint;
    }
}
