package io.meyer1994;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.philippheuer.events4j.core.EventManager;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.chat.ITwitchChat;
import com.github.twitch4j.common.util.EventManagerUtils;

final class TestTwitchChat implements ITwitchChat {
    private final EventManager eventManager = EventManagerUtils.initializeEventManager(SimpleEventHandler.class);
    private final Set<String> channels = new HashSet<>();
    private final Map<String, String> channelIdToName = new HashMap<>();
    private final Map<String, String> channelNameToId = new HashMap<>();
    private boolean closed;

    @Override
    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public void joinChannel(String channelName) {
        channels.add(channelName);
    }

    @Override
    public boolean leaveChannel(String channelName) {
        return channels.remove(channelName);
    }

    @Override
    public boolean sendMessage(String channelName, String message, Map<String, Object> tags) {
        throw new UnsupportedOperationException("Test client is read-only");
    }

    @Override
    public Set<String> getChannels() {
        return Set.copyOf(channels);
    }

    @Override
    public void close() {
        closed = true;
        eventManager.close();
    }

    @Override
    public long getLatency() {
        return 0;
    }

    @Override
    public Map<String, String> getChannelIdToChannelName() {
        return channelIdToName;
    }

    @Override
    public Map<String, String> getChannelNameToChannelId() {
        return channelNameToId;
    }

    boolean isClosed() {
        return closed;
    }
}
