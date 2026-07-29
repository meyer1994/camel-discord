package io.meyer1994;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

class KickComponentDiscoveryTest extends CamelTestSupport {

    @Test
    void discoversKickComponentByScheme() {
        assertNotNull(context.getComponent("kick"));
    }

    @Test
    void publishesKickComponentMetadata() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/io/meyer1994/kick.json")) {
            assertNotNull(stream);
            JsonNode metadata = new ObjectMapper().readTree(stream);

            assertEquals("kick", metadata.path("component").path("scheme").asText());
            assertEquals(true, metadata.path("component").path("consumerOnly").asBoolean());
            assertEquals("CHAT", metadata.path("properties").path("event").path("defaultValue").asText());
            assertEquals(5000, metadata.path("properties").path("reconnectDelay").path("defaultValue").asInt());
            assertEquals(10000,
                    metadata.path("properties").path("connectionTimeout").path("defaultValue").asInt());
            assertEquals("Long",
                    metadata.path("headers").path("x-camel-kick-chatroom-id").path("javaType").asText());
            assertEquals("String",
                    metadata.path("headers").path("x-camel-kick-user-name").path("javaType").asText());
        }
    }
}
