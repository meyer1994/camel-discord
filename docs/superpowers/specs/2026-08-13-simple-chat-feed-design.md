# Simple Chat Feed Design

## Goal

Reduce the example web UI to one focused page: a small form selects a
configured Twitch or Kick channel, then the page displays that channel's live
chat feed.

## Scope

The root page (`/`) is the only user-facing example page. The reusable `lib`
module and the example's Camel ingestion, persistence, and analytics routes are
unchanged. Existing channels remain limited to the values configured in
`application.properties`.

## Design

The index page renders a channel input, a Twitch/Kick platform selector, and a
submit button. The form uses HTMX to request a feed fragment and replace the
feed area without a full-page reload. A direct request with `platform` and
`channel` query parameters renders the same selected feed, so a feed can be
refreshed or bookmarked.

The controller validates the submitted channel case-insensitively against the
configured list for the selected platform. Invalid platform or channel values
render an inline error and do not open an SSE stream.

The SSE endpoint accepts the selected platform and subscribes to only the
corresponding existing topic: Twitch feeds use `Twitch.stream`, and Kick feeds
use `Kick.stream`. The feed fragment keeps the current HTMX SSE extension,
appends incoming message HTML, and scrolls to the newest message.

The layout is centered with a constrained maximum width. The feed has an
independent viewport-height scroll area and uses responsive CSS so it fills
narrow screens without horizontal overflow.

The chart, search, and chatter dashboard markup is removed from the web UI,
along with their page-specific controller mappings and templates. The
analytics Camel routes and persistence code remain available to avoid changing
the example's data pipeline beyond the requested UI simplification.

## Error handling

Missing or invalid form values show a concise inline validation message. A
valid selection renders the feed; SSE connection failures continue to use the
HTMX SSE extension's normal reconnect behavior. Message content remains escaped
by the existing Twitch and Kick renderers.

## Testing and verification

Add focused controller tests covering valid Twitch selection, valid Kick
selection, invalid channel rejection, and platform-specific stream selection.
Add template-level assertions where practical for the form fields, HTMX
attributes, and responsive feed structure. Run the example test suite and the
root Maven verification command relevant to the changed module.

## Alternatives considered

- Keeping the existing dashboard pages would leave the example's primary UI
  more complex than requested.
- Merging Twitch and Kick streams for a selected channel would ignore the
  platform choice and could show messages from the wrong source.
- Removing the persistence and analytics Camel routes would expand the change
  beyond simplifying the example's web UI.
