package io.meyer1994;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.twitch4j.chat.ITwitchChat;
import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

class TwitchEndpointTest extends CamelTestSupport {

    @Test
    void normalizesChannelLoginFromUri() {
        TwitchEndpoint endpoint = context.getEndpoint("twitch:TwItCh", TwitchEndpoint.class);

        assertEquals("twitch", endpoint.getChannel());
    }

    @Test
    void rejectsMissingChannelLogin() {
        assertThrows(ResolveEndpointFailedException.class, () -> context.getEndpoint("twitch:"));
    }

    @Test
    void rejectsProducerCreation() {
        TwitchEndpoint endpoint = context.getEndpoint("twitch:twitch", TwitchEndpoint.class);

        assertThrows(UnsupportedOperationException.class, endpoint::createProducer);
    }

    @Test
    void autowiresChatClientFromCamelRegistry() {
        ITwitchChat client = new TestTwitchChat();
        context.getRegistry().bind("twitchClient", client);

        TwitchEndpoint endpoint = context.getEndpoint("twitch:twitch", TwitchEndpoint.class);

        assertSame(client, endpoint.getClient());
    }
}
