package io.meyer1994;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;

@Component("discord")
public class DiscordComponent extends DefaultComponent {
    
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Endpoint endpoint = new DiscordEndpoint(uri, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }
}
