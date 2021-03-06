package io.meyer1994;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class DiscordComponentTest extends CamelTestSupport {

    @Test
    public void testDiscordEndpoint() throws Exception {
        DiscordComponent component = this.context
                .getComponent("discord", DiscordComponent.class);
        DiscordEndpoint endpoint = (DiscordEndpoint) component
                .createEndpoint("discord:banana?operation=MESSAGE_REPLY&event=ON_MESSAGE");

        assertEquals(endpoint.getOperation(), DiscordOperation.MESSAGE_REPLY);
        assertEquals(endpoint.getEvent(), DiscordEvent.ON_MESSAGE);
        assertEquals(endpoint.getName(), "banana");
    }
}
