# wikilayer-client-kotlin

[![Tests](https://github.com/wikilayer/wikilayer-client-kotlin/actions/workflows/tests.yml/badge.svg)](https://github.com/wikilayer/wikilayer-client-kotlin/actions/workflows/tests.yml)
[![Documentation](https://github.com/wikilayer/wikilayer-client-kotlin/actions/workflows/documentation.yml/badge.svg)](https://wikilayer.github.io/wikilayer-client-kotlin/)

The Kotlin/Android client for Wikilayer's API. It is a port of
[wikilayer-client-swift](https://github.com/wikilayer/wikilayer-client-swift),
which leads the shared behavior.

The library owns the requests and responses that cross the network. It does not
own a local database, screen state, or background scheduling.

Add JitPack and the library dependency:

```kotlin
repositories { maven("https://jitpack.io") }

dependencies {
    implementation("com.github.wikilayer:wikilayer-client-kotlin:0.2.0")
}
```

Create the APIs from the bundled host configuration:

```kotlin
val hosts = WikiHostConfiguration.bundled.pool()
val api = WikiApi(hosts)
val directory = api.wikis(matching = "markdown")
val batch = api.sync(wikiId = 2982, after = null)
```

## Host configuration

The library ships the primary server and any mirrors in `hosts.yaml`; applications
use `WikiHostConfiguration.bundled` instead of copying those addresses. Safe reads
can move to another host after a retryable failure, while one-use authentication
credentials remain bound to one selected host. The
[Dokka API reference](https://wikilayer.github.io/wikilayer-client-kotlin/)
describes host selection, authentication, synchronization, and live changes.

## Development

```sh
make test-build
make test
make lint
make docs
make build
```

## Lines of Code

<picture>
  <source media="(prefers-color-scheme: dark)" srcset=".github/loc-history-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset=".github/loc-history-light.svg">
  <img src=".github/loc-history.svg" alt="Lines of code over time">
</picture>
