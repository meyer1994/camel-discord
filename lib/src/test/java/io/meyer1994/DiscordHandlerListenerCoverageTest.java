package io.meyer1994;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.session.SessionInvalidateEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordHandlerListenerCoverageTest extends CamelTestSupport {

    @Test
    void everyConcreteListenerCallbackMapsToAnEvent() {
        Map<String, Method> callbacks = listenerCallbacks();

        for (DiscordEvent event : DiscordEvent.values()) {
            Method callback = callbacks.get(event.name());
            assertNotNull(callback, "No ListenerAdapter callback exists for " + event.name());
            assertEquals(event, DiscordEvent.fromEventType(callback.getParameterTypes()[0]));
        }

        assertEquals(callbacks.keySet(), Arrays.stream(DiscordEvent.values())
                .map(Enum::name)
                .collect(Collectors.toSet()),
                "DiscordEvent must cover every concrete ListenerAdapter callback");
        assertTrue(Arrays.stream(DiscordHandler.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("on"))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .allMatch(method -> method.getName().equals("onGenericEvent")));
    }

    @Test
    void matchingNewCallbacksDispatchTheOriginalEventAndEventHeader() throws Exception {
        JDA jda = testJda();
        assertDispatches(new SessionInvalidateEvent(jda), "onSessionInvalidate");
        assertDispatches(new SessionResumeEvent(jda), "onSessionResume");
    }

    @Test
    void nonmatchingNewCallbackIsIgnored() throws Exception {
        AtomicReference<Exchange> received = new AtomicReference<>();
        DiscordEndpoint endpoint = endpoint("onSessionResume");
        DiscordHandler handler = handler(endpoint, received::set);

        handler.onGenericEvent(new SessionInvalidateEvent(testJda()));

        assertNull(received.get(), "A nonmatching callback must not dispatch an exchange");
    }

    private void assertDispatches(GenericEvent event, String eventName) throws Exception {
        AtomicReference<Exchange> received = new AtomicReference<>();
        DiscordHandler handler = handler(endpoint(eventName), received::set);

        handler.onGenericEvent(event);

        Exchange exchange = received.get();
        assertNotNull(exchange);
        assertSame(event, exchange.getMessage().getBody());
        assertEquals(eventName, exchange.getMessage().getHeader(DiscordConstants.HEADER_EVENT));
    }

    private DiscordHandler handler(DiscordEndpoint endpoint, Processor processor) {
        return new DiscordHandler(new DiscordConsumer(endpoint, processor));
    }

    private DiscordEndpoint endpoint(String eventName) throws Exception {
        DiscordEndpoint endpoint = (DiscordEndpoint) createCamelContext()
                .getComponent("discord", DiscordComponent.class)
                .createEndpoint("discord:listener-tests");
        endpoint.setEvent(Enum.valueOf(DiscordEvent.class, eventName));
        return endpoint;
    }

    private static Map<String, Method> listenerCallbacks() {
        return Arrays.stream(ListenerAdapter.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getName().startsWith("on"))
                .filter(method -> !method.getName().startsWith("onGeneric"))
                .filter(method -> !method.getName().equals("onEvent"))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> GenericEvent.class.isAssignableFrom(method.getParameterTypes()[0]))
                .collect(Collectors.toMap(Method::getName, Function.identity()));
    }

    private static JDA testJda() {
        return (JDA) java.lang.reflect.Proxy.newProxyInstance(
                JDA.class.getClassLoader(),
                new Class<?>[] { JDA.class },
                (proxy, method, args) -> method.getReturnType().isPrimitive()
                        ? primitiveDefault(method.getReturnType())
                        : null);
    }

    private static Object primitiveDefault(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0d;
        }
        return null;
    }
}
