# Changelog

## 0.2.0

### Added

- `SyncNode.pageId` names the page a node belongs to, as the server works it
  out: a page answers with itself, a block with its nearest page ancestor. A
  page's document is the nodes carrying its identifier, so it stops at a page
  nested inside it.
- `WikiSummary`, `MyWiki` and `ResolvedAddress` carry the wiki's icon and
  whether it keeps pages under pages: `iconUrl` / `wikiIconUrl` and
  `pagesTree` / `wikiPagesTree`.

### Removed

- `NodePath.page`. A path carries identifiers and no kinds, so it could only
  ever answer with the first node below the wiki, which is the wrong page for
  anything inside a nested one. Read `SyncNode.pageId` instead:

  ```kotlin
  // was
  val page = node.nodePath.page
  // now
  val page = node.pageId
  ```

  A store that derived its own page column from the path holds the same
  mistake and has to be rebuilt from the synchronized values.

### Requires

- A server from 21 September 2026 or later. Against an older one every
  `pageId` reads 0.

## 0.1.5

### Added

- The account API reports the server's refusal to close an account that still
  owns a live wiki, so the app can say which wikis are in the way.

## 0.1.4

### Added

- `closeAccount` for deleting an account from the app.

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
