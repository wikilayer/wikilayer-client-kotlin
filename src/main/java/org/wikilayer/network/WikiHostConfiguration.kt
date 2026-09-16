package org.wikilayer.network

/** Hosts shipped as part of the client library. */
data class WikiHostConfiguration(
    val primary: String,
    val mirrors: List<String> = emptyList(),
) {
    fun pool(
        preferred: String? = null,
        didSelect: (String) -> Unit = {},
    ): WikiHostPool = WikiHostPool(primary, mirrors, preferred, didSelect = didSelect)

    companion object {
        /** The library-owned `hosts.yaml` configuration. */
        val bundled: WikiHostConfiguration by lazy {
            val text =
                checkNotNull(WikiHostConfiguration::class.java.getResourceAsStream("/hosts.yaml")) {
                    "hosts.yaml is not in the Wikilayer client library"
                }.bufferedReader().use { it.readText() }
            parse(text)
        }

        internal fun parse(yaml: String): WikiHostConfiguration {
            val meaningful =
                yaml
                    .lineSequence()
                    .map { it.substringBefore('#').trim() }
                    .filter(String::isNotEmpty)
                    .toList()
            val primary =
                meaningful
                    .firstOrNull { it.startsWith("primary:") }
                    ?.substringAfter(':')
                    ?.trim()
                    ?.takeIf { it.startsWith("https://") }
                    ?: error("hosts.yaml has no HTTPS primary host")
            val mirrorStart = meaningful.indexOfFirst { it.startsWith("mirrors:") }
            val mirrors =
                if (mirrorStart < 0) {
                    emptyList()
                } else {
                    meaningful
                        .drop(mirrorStart + 1)
                        .takeWhile { it.startsWith('-') }
                        .map { it.removePrefix("-").trim() }
                        .onEach { require(it.startsWith("https://")) { "mirror host must use HTTPS: $it" } }
                }
            return WikiHostConfiguration(primary, mirrors)
        }
    }
}
