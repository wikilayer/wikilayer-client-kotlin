package org.wikilayer.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WikiChannelTest {
    private lateinit var server: MockWebServer

    @Before
    fun start() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun stop() = server.close()

    private fun channel(): WikiChannel =
        WikiChannel(
            baseUrl = server.url("/").toString().trimEnd('/'),
            client = OkHttpClient(),
            firstRetryMillis = FIRST_RETRY,
            longestRetryMillis = LONGEST_RETRY,
        )

    private fun answer(
        body: String,
        code: Int = 200,
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .body(body)
            .build(),
    )

    @Test
    fun `each announced change arrives once`() =
        runBlocking {
            answer(
                """
                :ok
                event: ping
                data: {}
                event: changed
                data: {"wiki_id":1}
                event: changed
                data: {"wiki_id":1}
                """.trimIndent() + "\n",
            )

            val seen = channel().changes(inWiki = 1).take(2).toList()

            assertEquals(2, seen.size)
        }

    @Test
    fun `a heartbeat on a quiet wiki is not a change`() =
        runBlocking {
            answer(
                """
                :ok
                event: ping
                data: {}
                """.trimIndent() + "\n",
            )

            val change = withTimeoutOrNull(QUIET_MILLIS) { channel().changes(inWiki = 1).first() }

            assertNull("a heartbeat sent the screens off to read the wiki again", change)
        }

    @Test
    fun `a connection that fails is dialled again rather than ending the stream`() =
        runBlocking {
            answer("", code = 503)
            answer(
                """
                :ok
                event: changed
                data: {"wiki_id":1}
                """.trimIndent() + "\n",
            )

            val change = withTimeoutOrNull(DIAL_AGAIN_MILLIS) { channel().changes(inWiki = 1).first() }

            assertNotNull("a refusal ended the stream, and the wiki stopped announcing itself", change)
        }

    private companion object {
        const val FIRST_RETRY = 10L
        const val LONGEST_RETRY = 20L
        const val QUIET_MILLIS = 300L
        const val DIAL_AGAIN_MILLIS = 3_000L
    }
}
