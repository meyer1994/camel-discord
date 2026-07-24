# AGENTS.md

## Project overview

This is an Apache Camel Discord component built with Maven and Java 21.

- `lib/` contains the reusable `io.meyer1994:camel-discord` library.
- `example/` contains the standalone `io.meyer1994:camel-discord-example` bot.
- The root `pom.xml` is the Maven parent and reactor for both modules.

## Build commands from the repository root

Run these commands from the repository root:

```sh
# Clean all build output
mvn clean

# Compile all modules
mvn compile

# Run all tests
mvn test

# Package all modules
mvn package

# Install the library and example artifacts in the local Maven repository
mvn install
```

Target a module while also building its reactor dependencies with `-am`:

```sh
# Build and install only lib and its required projects
mvn -pl lib -am install

# Build and install example plus lib
mvn -pl example -am install

# Run tests for lib
mvn -pl lib -am test

# Run tests for example
mvn -pl example -am test
```

Useful lifecycle combinations include `clean compile`, `clean test`,
`clean package`, and `clean install`, for example:

```sh
mvn clean install
mvn -pl lib -am clean package
mvn -pl example -am clean package
```

## `lib` module

The library is the `camel-discord` JAR. From `lib/`, use:

```sh
mvn clean
mvn compile
mvn test
mvn package
mvn install
```

The library build also generates Camel component metadata during the Maven
build. Prefer the root command `mvn -pl lib -am <phase>` when working from a
fresh checkout so Maven can resolve the parent and reactor context together.

### How the library works

`DiscordComponent` creates `DiscordEndpoint` instances for `discord:` URIs.
Each endpoint can create a consumer or producer. Consumers register a
`DiscordHandler` with the configured JDA client; the handler converts selected
JDA events into Camel exchanges with Discord headers and event data. Producers
read the exchange body and headers to send messages, reply to messages, or add
reactions. Stopping a consumer removes its JDA listener.

## `example` module

The example depends on the library. From a fresh checkout, build it together
with `lib` from the repository root:

```sh
mvn -pl example -am clean install
```

From `example/`, the standard Maven lifecycle commands are:

```sh
mvn clean
mvn compile
mvn test
mvn package
mvn install
```

After installing the modules, run the Camel example with a Discord bot token:

```sh
DISCORD_TOKEN=YOUR_DISCORD_BOT_TOKEN mvn -pl example camel:run
```

Do not commit real Discord tokens. Use an environment variable or another
local secret-management mechanism when running the example.

## Development notes

- Use Java 21 or newer; the parent POM sets `maven.compiler.release` to `21`.
- Keep generated files under `lib/src/generated/` consistent with the source
  and Maven component metadata generation.
- Check the existing working tree before editing; preserve unrelated user
  changes.
