package io.meyer1994;

import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

@Component("twitch")
public class TwitchComponent extends DefaultComponent {

  @Override
  protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
      throws Exception {
    TwitchEndpoint endpoint = new TwitchEndpoint(uri, this);
    endpoint.setChannel(remaining);
    setProperties(endpoint, parameters);
    return endpoint;
  }
}
