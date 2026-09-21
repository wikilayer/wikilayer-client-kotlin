# Wikilayer Client for Kotlin

The Android client for Wikilayer's HTTP, authentication, synchronization, and
live-update APIs. The library owns the server and mirror configuration in
`hosts.yaml`; applications use ``org.wikilayer.network.WikiHostConfiguration.bundled``.

Safe reads may move to a configured mirror after a network failure or HTTP 451.
One-use identity tokens, authorization codes, and sign-out requests are never
retried on a different host.

## Synchronizing wikis

Call `WikiApi.sync` with `null` for the first page. Apply every `SyncNode` in the
returned order, then persist `SyncBatch.cursor` only after the whole page has
been stored. While `SyncBatch.hasMore` is true, request the next page immediately
with that cursor.

A node whose `deleted` value is true removes the local record with the same
identifier. Other nodes replace the locally stored representation. The overload
without an explicit limit uses the `syncPageSize` supplied to `WikiApi`.

The wiki's own node carries what the wiki looks like: `SyncNode.iconUrl` and
`SyncNode.pagesTree`. A reader who follows a wiki rather than owning it is listed
it nowhere, so this is the answer that reaches every wiki held.

`SyncNode.pageId` names the page a node belongs to: a page answers with itself,
and a block with its nearest page ancestor. A page's document is the nodes
carrying its identifier, which is what stops that document at a page nested
inside it. `SyncNode.path` cannot answer this, because it carries identifiers
and no kinds, so the server is the side that works it out.

Cancellation is returned unchanged. Retryable host failures may select a mirror;
if every candidate fails, the call throws `WikiApiError.Unreachable`.

## Live changes

`WikiChannel.changes` is a signal to synchronize, not a stream of wiki data. Each
element means the application should run the normal cursor-based synchronization
pass. Several changes may be represented by one signal, and reconnecting does not
replay missed signals, so the synchronization cursor remains the source of
completeness.

The channel reconnects after a failure with exponential backoff between its first
and longest retry intervals. It calls `onFailure` before waiting. Cancelling
collection closes the network call without reporting a failure. The `Signing`
callback is evaluated for every connection attempt, so a renewed or removed
credential takes effect after reconnecting.

## Authentication

Call `AuthApi.prepareHost` before opening the Apple or Google sign-in interface.
The preflight selects a reachable host before the provider creates a one-use
identity token. Send that token once with `AuthApi.signIn`.

For browser OAuth, create a `Pkce` value and one `AuthorizationRequest`. The
request remembers its issuing host internally, and `AuthApi.exchange` sends the
code only to that host.
