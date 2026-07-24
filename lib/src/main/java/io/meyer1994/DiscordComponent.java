package io.meyer1994;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

@Component("discord")
public class DiscordComponent extends DefaultComponent {
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        DiscordEndpoint endpoint = new DiscordEndpoint(uri, this);
        endpoint.name = remaining;
        this.setProperties(endpoint, parameters);
        return endpoint;
    }
}
