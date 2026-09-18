# Changelog

## 0.1.3

### Changed

- Expanded the API reference with the contracts for synchronization, live
  changes, authentication, host selection, failures, and wire models. No
  application changes are required when updating.

## 0.1.2

### Fixed

- Cancellation is returned to the caller instead of being reported as an
  unreachable host.

## 0.1.1

### Fixed

- Verify that the bundled host configuration matches the leading Swift port.
- Repair the Android SDK setup used by continuous integration.

## 0.1.0

### Added

- Requests and response models for the public directory, address resolution,
  synchronization, and authentication.
- A live change channel for wikis.
- Keep the primary host and ordered mirrors inside the library's `hosts.yaml`.
- Ordered hosts with automatic failover for safe requests and a typed failure
  for every host attempted.
