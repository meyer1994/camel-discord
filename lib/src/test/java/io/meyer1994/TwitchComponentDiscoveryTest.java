package io.meyer1994;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

class TwitchComponentDiscoveryTest extends CamelTestSupport {

  @Test
  void discoversTwitchComponentByScheme() {
    assertNotNull(context.getComponent("twitch"));
  }

  @Test
  void publishesOnlyTwitchComponentMetadata() throws Exception {
    try (InputStream stream =
        getClass().getResourceAsStream("/META-INF/io/meyer1994/twitch.json")) {
      assertNotNull(stream);
      String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(metadata.contains("\"consumerOnly\": true"));
      assertTrue(metadata.contains("\"javaType\": \"com.github.twitch4j.ITwitchClient\""));
      assertTrue(metadata.contains("\"x-camel-twitch-message-id\""));
    }

    assertNull(getClass().getResource("/META-INF/io/meyer1994/discord.json"));
  }
}
