package io.meyer1994;

import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiscordComponentTest extends CamelTestSupport {

    @Test
    public void testDiscordEndpoint() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:reply");

        assertEquals(endpoint.getName(), "reply");
    }

    @Test
    public void testDiscordComponentIsDiscoveredByCamel() throws Exception {
        assertInstanceOf(DiscordComponent.class, createCamelContext().getComponent("discord"));
    }

    @Test
    public void testGeneratedMetadataUsesCurrentComponentDescriptionAndCategory() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/io/meyer1994/discord.json")) {
            String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(metadata.contains("\"label\": \"social\""));
            assertTrue(metadata.contains("Apache Camel endpoint for receiving Discord messages"));
            assertTrue(!metadata.contains("\"event\""));
        }
    }

    @Test
    public void testListenerMethodNameCanBeConfigured() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:onMessageUpdate");

        assertEquals(endpoint.getName(), "onMessageUpdate");
        assertDoesNotThrow(() -> endpoint.createConsumer(exchange -> {
        }));
    }

    @Test
    public void testProducerMethodNameCanBeConfigured() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:addReactionById");

        assertEquals(endpoint.getName(), "addReactionById");
    }

    @Test
    public void testArbitraryListenerNamesAreRejected() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:ping");

        assertThrows(IllegalArgumentException.class, () -> endpoint.createConsumer(exchange -> {
        }));
    }
}
