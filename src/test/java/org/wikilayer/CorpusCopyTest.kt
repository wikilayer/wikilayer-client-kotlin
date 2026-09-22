package org.wikilayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * The cases belong to the leading port and live here as a copy that `make sync-yaml`
 * refreshes. Nobody reads that repository at run time, so a copy left behind would keep
 * this port on older behaviour with every test still green: the case that never arrived
 * is a case these tests do not know.
 *
 * So the copies are held against the leading repository itself. A file that has moved on
 * there fails here, and so does a file that was never copied at all.
 */
class CorpusCopyTest {
    @Test
    fun `the cases match the leading port`() {
        for ((there, here) in SHARED) {
            assertEquals(
                "$here differs from the leading port: run `make sync-yaml`",
                fetch(there),
                copy(here),
            )
        }
    }

    private fun fetch(path: String): String {
        val address = "https://raw.githubusercontent.com/wikilayer/wikilayer-client-swift/main/$path"
        val request =
            HttpRequest
                .newBuilder(URI.create(address))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build()
        val answer = CLIENT.send(request, HttpResponse.BodyHandlers.ofString())
        assertTrue("$address answered ${answer.statusCode()}", answer.statusCode() == OK)
        return answer.body()
    }

    private fun copy(path: String): String =
        requireNotNull(CorpusCopyTest::class.java.getResourceAsStream(path)) {
            "$path is missing: run `make sync-yaml`"
        }.use { it.readBytes().decodeToString() }

    private companion object {
        const val OK = 200
        const val TIMEOUT_SECONDS = 10L
        val CLIENT: HttpClient = HttpClient.newHttpClient()

        val SHARED =
            listOf(
                "Tests/WikilayerClientTests/Resources/page_tree_tests.yaml" to "/page_tree_tests.yaml",
            )
    }
}
