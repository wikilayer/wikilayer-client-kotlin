# Changelog

## 0.1.1

- Verify that the bundled host configuration matches the leading Swift port.
- Repair the Android SDK setup used by continuous integration.

## 0.1.0

- Extract the Android Wikilayer API, authentication, sync, and live-channel client.
- Keep the primary host and ordered mirrors inside the library's `hosts.yaml`.
- Fail over safe requests while keeping one-use credentials bound to one host.
- Publish API reference documentation with Dokka.
