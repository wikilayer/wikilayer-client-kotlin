# Wikilayer Client for Kotlin

The Android client for Wikilayer's HTTP, authentication, synchronization, and
live-update APIs. The library owns the server and mirror configuration in
`hosts.yaml`; applications use ``org.wikilayer.network.WikiHostConfiguration.bundled``.

Safe reads may move to a configured mirror after a network failure or HTTP 451.
One-use identity tokens, authorization codes, and sign-out requests are never
retried on a different host.
