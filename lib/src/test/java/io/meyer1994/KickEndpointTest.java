package io.meyer1994;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.camel.ResolveEndpointFailedException;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

class KickEndpointTest extends CamelTestSupport {

    @Test
    void normalizesChannelAndDefaultsToChat() {
        KickEndpoint endpoint = context.getEndpoint("kick:XqC", KickEndpoint.class);

        assertEquals("xqc", endpoint.getChannel());
        assertEquals(KickEvent.CHAT, endpoint.getEvent());
        assertNull(endpoint.getChatroomId());
        assertEquals(5_000, endpoint.getReconnectDelay());
        assertEquals(10_000, endpoint.getConnectionTimeout());
    }

    @Test
    void bindsChatroomAndConnectionOptions() {
        KickEndpoint endpoint = context.getEndpoint(
                "kick:xqc?event=CHAT&chatroomId=668&reconnectDelay=2500&connectionTimeout=3000",
                KickEndpoint.class);

        assertEquals(668L, endpoint.getChatroomId());
        assertEquals(2_500, endpoint.getReconnectDelay());
        assertEquals(3_000, endpoint.getConnectionTimeout());
    }

    @Test
    void rejectsMissingChannel() {
        assertThrows(ResolveEndpointFailedException.class, () -> context.getEndpoint("kick:"));
    }

    @Test
    void rejectsNonPositiveChatroomId() {
        assertThrows(ResolveEndpointFailedException.class,
                () -> context.getEndpoint("kick:xqc?chatroomId=0"));
    }

    @Test
    void rejectsNegativeReconnectDelay() {
        assertThrows(ResolveEndpointFailedException.class,
                () -> context.getEndpoint("kick:xqc?reconnectDelay=-1"));
    }

    @Test
    void rejectsNonPositiveConnectionTimeout() {
        assertThrows(ResolveEndpointFailedException.class,
                () -> context.getEndpoint("kick:xqc?connectionTimeout=0"));
    }

    @Test
    void rejectsProducerCreation() {
        KickEndpoint endpoint = context.getEndpoint("kick:xqc", KickEndpoint.class);

        assertThrows(UnsupportedOperationException.class, endpoint::createProducer);
    }

    @Test
    void createsKickConsumer() throws Exception {
        KickEndpoint endpoint = context.getEndpoint("kick:xqc", KickEndpoint.class);

        assertInstanceOf(KickConsumer.class, endpoint.createConsumer(exchange -> {
        }));
    }
}
