# Camel Twitch Component

[![build](https://github.com/meyer1994/camel-discord/actions/workflows/build.yml/badge.svg)](https://github.com/meyer1994/camel-discord/actions/workflows/build.yml)

An Apache Camel component for consuming Twitch chat messages through
[Twitch4J](https://twitch4j.github.io/).

## Requirements

- Java 21 or newer
- Apache Camel 4.x
- Maven

## Project layout

- `lib/` contains the reusable `io.meyer1994:camel-twitch` component.
- `example/` contains a Spring Boot web example that streams Twitch chat to a
  browser.

## Build

```sh
mvn clean install
```

## Usage

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
