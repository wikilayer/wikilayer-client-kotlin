package org.wikilayer.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WikiHostConfigurationTest {
    @Test
    fun `the bundled host belongs to the library`() {
        assertEquals("https://wikilayer.org", WikiHostConfiguration.bundled.primary)
        assertTrue(WikiHostConfiguration.bundled.mirrors.isEmpty())
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
