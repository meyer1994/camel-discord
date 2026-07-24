package io.meyer1994;

import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiscordComponentTest extends CamelTestSupport {

    @Test
    public void testDiscordEndpoint() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:messages?event=onMessageReceived");

        assertEquals("messages", endpoint.getName());
        assertEquals(DiscordEvent.onMessageReceived, endpoint.getEvent());
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
            assertTrue(metadata.contains("\"event\""));
            assertTrue(metadata.contains("\"operation\""));
        }
    }

    @Test
    public void testEventCanBeConfiguredOnNamedEndpoint() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:message-events?event=onMessageUpdate");

        assertEquals("message-events", endpoint.getName());
        assertEquals(DiscordEvent.onMessageUpdate, endpoint.getEvent());
        assertDoesNotThrow(() -> endpoint.createConsumer(exchange -> {
        }));
    }

    @Test
    public void testOperationCanBeConfiguredOnNamedEndpoint() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:message-actions?operation=addReactionById");

        assertEquals("message-actions", endpoint.getName());
        assertEquals(DiscordOperation.addReactionById, endpoint.getOperation());
    }

    @Test
    public void testEndpointNameIsIndependentFromEvent() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:arbitrary-name?event=onMessageDelete");

        assertEquals("arbitrary-name", endpoint.getName());
        assertDoesNotThrow(() -> endpoint.createConsumer(exchange -> {
        }));
    }
}
