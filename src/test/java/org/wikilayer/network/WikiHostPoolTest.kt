package org.wikilayer.network

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikilayer.network.model.Credential

@RunWith(RobolectricTestRunner::class)
class WikiHostPoolTest {
    @Test
    fun `a failed primary advances to the mirror and remembers it`() =
        runTest {
            val primary = MockWebServer()
            val mirror = MockWebServer()
            primary.start()
            mirror.start()
            try {
                primary.enqueue(MockResponse.Builder().code(451).build())
                mirror.enqueue(wikis())
                mirror.enqueue(wikis())
                val api = WikiApi(pool(primary, mirror), OkHttpClient(), jacksonObjectMapper())

                api.wikis()
                api.wikis()

                assertEquals(1, primary.requestCount)
                assertEquals(2, mirror.requestCount)
            } finally {
                primary.close()
                mirror.close()
            }
        }

    @Test
    fun `a one-shot identity token is never retried on a mirror`() {
        val primary = MockWebServer()
        val mirror = MockWebServer()
        primary.start()
        mirror.start()
        try {
            primary.enqueue(MockResponse.Builder().code(451).build())
            val auth = auth(pool(primary, mirror))

            assertThrows(WikiApiError.Status::class.java) {
                runTest { auth.signIn("one-shot") }
            }
            assertEquals(1, primary.requestCount)
            assertEquals(0, mirror.requestCount)
        } finally {
            primary.close()
            mirror.close()
        }
    }

    @Test
    fun `a safe preflight selects the mirror before the one-shot token exists`() =
        runTest {
            val primary = MockWebServer()
            val mirror = MockWebServer()
            primary.start()
            mirror.start()
            try {
                primary.enqueue(MockResponse.Builder().code(451).build())
                mirror.enqueue(wikis())
                mirror.enqueue(MockResponse.Builder().body("""{"access_token":"ours"}""").build())
                val auth = auth(pool(primary, mirror))

                auth.prepareHost()
                auth.signIn("one-shot")

                assertEquals(1, primary.requestCount)
                assertEquals(2, mirror.requestCount)
            } finally {
                primary.close()
                mirror.close()
            }
        }

    @Test
    fun `authorization exchange stays bound to its original host`() {
        val primary = MockWebServer()
        val mirror = MockWebServer()
        primary.start()
        mirror.start()
        try {
            primary.enqueue(MockResponse.Builder().code(451).build())
            val auth = auth(pool(primary, mirror))
            val request = requireNotNull(auth.authorizationRequest("github", "state", "challenge"))

            assertThrows(WikiApiError.Status::class.java) {
                runTest { auth.exchange("one-shot", "verifier", request) }
            }
            assertEquals(1, primary.requestCount)
            assertEquals(0, mirror.requestCount)
        } finally {
            primary.close()
            mirror.close()
        }
    }

    @Test
    fun `signing out is never repeated on a mirror`() {
        val primary = MockWebServer()
        val mirror = MockWebServer()
        primary.start()
        mirror.start()
        try {
            primary.enqueue(MockResponse.Builder().code(451).build())
            val auth = auth(pool(primary, mirror))

            assertThrows(WikiApiError.Status::class.java) {
                runTest { auth.signOut(Credential("ours")) }
            }
            assertEquals(1, primary.requestCount)
            assertEquals(0, mirror.requestCount)
        } finally {
            primary.close()
            mirror.close()
        }
    }

    private fun pool(
        primary: MockWebServer,
        mirror: MockWebServer,
    ) = WikiHostPool(
        primary = primary.url("/").toString(),
        mirrors = listOf(mirror.url("/").toString()),
    )

    private fun auth(pool: WikiHostPool) =
        AuthApi(
            pool,
            OkHttpClient(),
            jacksonObjectMapper(),
            OAuthClient("app", "app:/oauth", "app"),
        )

    private fun wikis() = MockResponse.Builder().body("""{"wikis":[],"has_more":false}""").build()
}
