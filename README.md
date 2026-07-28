# Camel Twitch and Kick Components

[![build](https://github.com/meyer1994/camel-discord/actions/workflows/build.yml/badge.svg)](https://github.com/meyer1994/camel-discord/actions/workflows/build.yml)

Apache Camel consumer components for Twitch and Kick chat. Twitch uses
[Twitch4J](https://twitch4j.github.io/); Kick uses Java's built-in HTTP and
WebSocket clients.

## Requirements

- Java 21 or newer
- Apache Camel 4.x
- Maven

## Project layout

- `lib/` contains the reusable `io.meyer1994:camel-twitch` artifact with the
  `twitch:` and `kick:` components.
- `example/` contains a Spring Boot web example that streams Twitch chat to a
  browser.

## Build

```sh
mvn clean install
```

## Twitch usage

Create a Twitch4J chat client and bind it to the Camel registry:

```java
ITwitchChat client = TwitchChatBuilder.builder()
        .withAutoJoinOwnChannel(false)
        .build();

main.bind("client", client);
```

The endpoint path is the Twitch channel login:

```java
from("twitch:twitch")
    .log("${body.user.name}: ${body.message}");
```

Each exchange body is a Twitch4J `ChannelMessageEvent`. The component also
provides channel, user, and message IDs through `x-camel-twitch-*` headers.

The first version is consumer-only and supports anonymous read-only chat.
Authentication, sending messages, Helix, and EventSub are outside its scope.

## Kick usage

The Kick component is anonymous, consumer-only, and currently supports chat
messages:

```java
from("kick:xqc")
    .log("${body.sender.username}: ${body.content}");
```

The body is a `KickChatMessage` record containing the message ID, chatroom ID,
content, type, creation timestamp, and `KickChatSender`.

The component normally resolves the chatroom ID from the channel slug through
Kick's internal `/api/v2` endpoint. Kick may block that lookup with Cloudflare;
in that case, supply the numeric chatroom ID directly:

```java
from("kick:xqc?chatroomId=668")
    .log("${header.x-camel-kick-user-name}: ${body.content}");
```

Kick message metadata is also available through `x-camel-kick-*` headers for
the channel, chatroom, user, message, and event type.

The Kick Pusher WebSocket and `/api/v2` endpoints are undocumented internal
interfaces and may change without notice. This component does not implement
Kick authentication, outbound chat, or official webhook subscriptions.

## Run the example

No token or environment variable is required:

```sh
mvn -pl example -am clean install
mvn -pl example spring-boot:run
```

Open `http://localhost:8080/?channel=cellbit`, replacing `cellbit` with the
channel login you want to watch. The page opens an SSE connection to
`/events?channel=cellbit`. Each browser connection creates a Camel
`from("twitch:cellbit")` route, and disconnecting removes that route.
