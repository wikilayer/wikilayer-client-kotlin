# Changelog

## 0.1.2

- A call that was stopped is no longer reported as a host that could not be
  reached. A screen that goes away while a sync is running cancels the call,
  and the caller gets that back as it was raised, rather than as a server
  being down.

## 0.1.1

- Verify that the bundled host configuration matches the leading Swift port.
- Repair the Android SDK setup used by continuous integration.

## 0.1.0

- Extract the Android Wikilayer API, authentication, sync, and live-channel client.
- Keep the primary host and ordered mirrors inside the library's `hosts.yaml`.
- Fail over safe requests while keeping one-use credentials bound to one host.
- Publish API reference documentation with Dokka.
