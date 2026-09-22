# Changelog

## 0.5.0

Ported from wikilayer-client-swift 0.5.0.

### Fixed

- Siblings whose sort keys reach past the basic plane came back in a different
  order here than in the other ports: strings compared as this platform stores
  them, by unit, and a character above that plane sorts below one written in a
  single unit. Sort keys are read by their code points now, and the shared corpus
  holds the case.

## 0.4.0

Ported from wikilayer-client-swift 0.4.0.

### Added

- The tree of pages a wiki keeps, which every app reading a wiki works out the
  same way and until now wrote for itself.

  ```kotlin
  data class PageInTree(
      val id: Long,
      val parentId: Long,   // the node it hangs from: the wiki itself at the top
      val title: String,
      val sortKey: String,  // SyncNode.sortKey, the server's order among siblings
      val isHome: Boolean,  // the page the wiki is read from
  )

  val tree: List<PageBranch> = pages.pageTree()
  ```

  Hand in the pages of one wiki in one language, including the home page, in any
  order: siblings come back by `sortKey` and then by id, and the home page leads
  whatever it sorts as, so two apps storing the same wiki draw the same tree.

  `PageBranch` carries `id`, `title` and `children: List<PageBranch>`. Nothing
  handed in is dropped: a page whose parent was not handed in is a root of its
  own, so are pages whose parents form a cycle, and a page repeated across two
  sync batches is kept once.

  ```kotlin
  tree.outline(expanded: Set<Long>)   // -> List<PageOutlineRow>
  tree.neighbours(of: Long)           // -> PageNeighbours?
  tree.ancestors(of: Long)            // -> List<Long>
  ```

  `PageOutlineRow` is `id`, `title`, `depth` counting from 0 at the top, and
  `hasChildren`, which tells a closed branch from a page with nothing under it.
  Only the branches named in `expanded` show their children, and which those are
  stays with the caller.

  `PageNeighbours` is `previous` and `next`, each a `PageStep` of `id` and
  `title`. The walk closes into a ring, so the last page leads back to the first
  rather than to a dead end. Fewer than three pages answer null, because with two
  both ways lead to the same page, and so does a page the tree does not hold:
  either way there is nowhere to step from here.

  `ancestors(of:)` lists the pages above the given one, outermost first, and does
  not include the page itself.

## 0.3.0

Ported from wikilayer-client-swift 0.3.0.

### Changed

- What a wiki looks like now travels on the wiki's own node in a sync:
  `SyncNode.iconUrl` and `SyncNode.pagesTree`. A reader who follows a wiki
  rather than owning it is listed it nowhere, so the account's listing could
  never tell them, and it no longer pretends to: `MyWiki` has lost `iconUrl`
  and `pagesTree`.
- `WikiSummary` and `ResolvedAddress` keep the icon and lose `pagesTree`: both
  answer about a wiki the reader does not hold yet, which is the only moment
  before its own node can speak.

### Requires

- A server from 21 September 2026 or later, second deploy of that day.

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
