# Discord Listener Expansion Design

## Objective

Expose the remaining JDA `ListenerAdapter` callbacks as selectable Camel consumer events while keeping all listener implementations in `DiscordHandler` and preserving the existing message and reaction behavior.

## Scope

The component currently supports eight message and reaction callbacks through `DiscordEvent` and explicit overrides in `DiscordHandler`. The implementation will add the remaining concrete JDA callbacks available in the project's JDA version, including lifecycle, interaction, user, channel, guild, thread, voice, moderation, role, emoji, sticker, soundboard, entitlement, and related events.

Generic catch-all callbacks are not separate selectable events unless they correspond to a concrete callback/event that can be configured unambiguously. Concrete callback names remain the public event API.

## Architecture

`DiscordEvent` will contain one enum value per supported JDA callback, using the callback method name (`onReady`, `onGuildJoin`, and so on) to remain consistent with the existing API.

`DiscordHandler` will explicitly override every supported callback. Each override will delegate to private helper methods in the same class. The helpers will:

1. Check the configured `DiscordEvent` and return when it does not match.
2. Create a Camel exchange from the consumer endpoint.
3. Set `DiscordConstants.HEADER_EVENT` to the selected event name.
4. Set the original JDA event object as the exchange body.
5. Add headers applicable to the concrete event type.
6. Pass the exchange through the existing `process(exchange)` method.

The existing message and reaction handlers will retain their current detailed headers and behavior. Shared helpers will reduce duplicated exchange setup without moving listener implementations out of `DiscordHandler`.

## Event and header behavior

Every supported callback will set `HEADER_EVENT` and use the original JDA event as the body. New callbacks will set only values safely available from their event type, including channel ID, channel type, guild ID, user ID, message ID, and guild/thread flags where applicable. Inapplicable values will be omitted rather than fabricated or set to null.

The enum expansion will be reflected in generated Camel endpoint metadata so the `event` URI parameter and documentation list all supported values.

## Testing and verification

Tests will cover representative callbacks from each event family and verify that:

- each representative event can be configured through the `event` URI parameter;
- nonmatching events are ignored;
- matching events produce an exchange;
- the original JDA event is the body;
- `HEADER_EVENT` is set correctly;
- applicable headers are populated;
- existing message and reaction behavior remains intact;
- generated metadata contains the expanded event enum.

The full Maven test suite and Camel metadata generation will be run after implementation.

## Compatibility

Existing endpoint names, producer operations, message/reaction event names, headers, and routing behavior remain unchanged. The change is additive for consumers, except that the generated `DiscordEvent` metadata will contain more allowed values.
