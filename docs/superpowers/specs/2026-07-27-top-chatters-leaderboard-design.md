# Top Chatters Leaderboard Design

## Goal

Add a compact dashboard leaderboard showing the 10 users with the most stored
messages for the selected Twitch channel.

## Scope

The ranking is channel-scoped and covers all messages stored in
`twitch_chat_messages`, matching the existing emoji and AI-category statistics.
The reusable `lib` Twitch component is unchanged.

## Design

Add a Camel direct route named `direct:topChatters` in the example module. The
route queries `twitch_chat_messages`, groups by `user_name`, counts rows, sorts
by count descending, and limits the result to 10 rows. The query is parameterized
by the requested channel.

Expose the route through a new JSON endpoint, `/api/stats/chatters/top`, with a
response shaped as:

```json
{
  "items": [
    { "user": "example_user", "count": 42 }
  ]
}
```

The controller converts JDBC numeric values to `long` values and preserves the
database ordering.

The dashboard adds a “Top chatters” card near the other summary/statistics
cards. It renders up to 10 rows with rank, username, and localized message
count. An empty result renders a concise “No data yet” state. The existing
five-second refresh cycle requests the new endpoint along with the other
statistics.

## Error handling

The feature follows the existing statistics behavior: database or route errors
are surfaced by the endpoint and logged by the dashboard refresh handler. User
names are inserted as text content so they cannot inject markup.

## Testing and verification

Add a focused test for the ranking result mapping/query behavior using the
example module’s existing test conventions. Verify that the top-10 limit,
descending count order, channel scope, and JSON field names are correct. Run
the example tests and the root Maven test/build commands relevant to the
change.

## Alternatives considered

- Combining the ranking with the existing time-series chatters endpoint would
  mix unrelated response shapes.
- Computing the ranking only from live events would omit historical stored
  messages and duplicate database aggregation.

