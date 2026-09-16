package org.wikilayer.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class WikiHostConfigurationTest {
    @Test
    fun `the bundled host belongs to the library`() {
        assertEquals("https://wikilayer.org", WikiHostConfiguration.bundled.primary)
        assertTrue(WikiHostConfiguration.bundled.mirrors.isEmpty())
    }

    @Test
    fun `the bundled hosts match the leading Swift port`() {
        val leading =
            URI(
                "https://raw.githubusercontent.com/wikilayer/" +
                    "wikilayer-client-swift/main/Sources/WikilayerClient/Resources/hosts.yaml",
            ).toURL().readText()
        val bundled =
            checkNotNull(WikiHostConfiguration::class.java.getResource("/hosts.yaml"))
                .readText()

        assertEquals(leading, bundled)
    }

    @Test
    fun `ordered mirrors are read from YAML`() {
        val hosts =
            WikiHostConfiguration.parse(
                """
                primary: https://wikilayer.org
                mirrors:
                  - https://one.example
                  - https://two.example
                """.trimIndent(),
            )

        assertEquals(listOf("https://one.example", "https://two.example"), hosts.mirrors)
    }

    @Test
    fun `a mirror cannot downgrade credentials to plain HTTP`() {
        assertThrows(IllegalArgumentException::class.java) {
            WikiHostConfiguration.parse(
                """
                primary: https://wikilayer.org
                mirrors:
                  - http://unsafe.example
                """.trimIndent(),
            )
        }
    }
}
