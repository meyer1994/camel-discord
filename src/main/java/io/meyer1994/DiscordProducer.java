package io.meyer1994;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordProducer extends DefaultProducer {
    private static final Logger LOG = LoggerFactory.getLogger(DiscordProducer.class);
    private DiscordEndpoint endpoint;

    public DiscordProducer(DiscordEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    public void process(Exchange exchange) throws Exception {
        System.out.println(exchange.getIn().getBody());
    }

}
