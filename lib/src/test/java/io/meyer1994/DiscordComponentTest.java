package io.meyer1994;

import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiscordComponentTest extends CamelTestSupport {

    @Test
    public void testDiscordEndpoint() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:banana?operation=MESSAGE_REPLY&event=ON_MESSAGE");

        assertEquals(endpoint.getOperation(), DiscordOperation.MESSAGE_REPLY);
        assertEquals(endpoint.getEvent(), DiscordEvent.ON_MESSAGE);
        assertEquals(endpoint.getName(), "banana");
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
        }
    }

    @Test
    public void testAdditionalDiscordEventsCanBeConfigured() throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) this
                .createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:commands?event=ON_MESSAGE_UPDATE");

        assertEquals(DiscordEvent.ON_MESSAGE_UPDATE, endpoint.getEvent());
    }
}
