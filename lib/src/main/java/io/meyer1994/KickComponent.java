package io.meyer1994;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

@Component("kick")
public class KickComponent extends DefaultComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        if (remaining == null || remaining.isBlank()) {
            throw new IllegalArgumentException("A Kick channel is required. Example: kick:xqc");
        }

        KickEndpoint endpoint = new KickEndpoint(uri, this);
        endpoint.setChannel(remaining);
        setProperties(endpoint, parameters);
        return endpoint;
    }
}
