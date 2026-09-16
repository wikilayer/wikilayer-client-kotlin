package org.wikilayer.network

import android.net.Uri
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikilayer.network.model.Credential

@RunWith(RobolectricTestRunner::class)
class AuthApiTest {
    private lateinit var server: MockWebServer
    private lateinit var auth: AuthApi
    private val oauth = OAuthClient("wikilayer-app", "wikilayer://auth", "app")

    @Before
    fun start() {
        server = MockWebServer()
        server.start()
        auth =
            AuthApi(
                baseUrl = server.url("/").toString().trimEnd('/'),
                client = OkHttpClient(),
                mapper =
                    ObjectMapper()
                        .registerKotlinModule()
                        .registerModule(JavaTimeModule())
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false),
                clientTheServerRegistered = oauth,
            )
    }

    @After
    fun stop() = server.close()

    private fun answer(
        body: String,
        code: Int = 200,
    ) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .body(body)
                .build(),
        )
    }

    @Test
    fun `the authorization address carries the challenge and the state it was made with`() {
        val opened = Uri.parse(auth.authorizationUrl(provider = "github", state = "st-1", challenge = "ch-1"))

        assertEquals("/oauth/authorize", opened.path)
        assertEquals(oauth.id, opened.getQueryParameter("client_id"))
        assertEquals(oauth.redirectUri, opened.getQueryParameter("redirect_uri"))
        assertEquals("github", opened.getQueryParameter("provider"))
        assertEquals("st-1", opened.getQueryParameter("state"))
        assertEquals("ch-1", opened.getQueryParameter("code_challenge"))
        assertEquals("S256", opened.getQueryParameter("code_challenge_method"))
    }

    @Test
    fun `Google's identity token is traded for a credential of ours`() =
        runTest {
            answer("""{"access_token":"tok-9"}""")

            val credential = auth.signIn("id-token-1")

            assertEquals("tok-9", credential.token)
            val asked = server.takeRequest()
            assertEquals("/api/auth/google", Uri.parse(asked.url.toString()).path)
            val sent =
                asked.body
                    ?.utf8()
                    .orEmpty()
            assertTrue("the identity token is the whole proof of who signed in: $sent", sent.contains("id-token-1"))
        }

    @Test
    fun `a code is traded for a credential with the verifier it was asked under`() =
        runTest {
            answer("""{"access_token":"tok-3"}""")

            val authorization = requireNotNull(auth.authorizationRequest("github", "state", "challenge"))
            val credential = auth.exchange(code = "code-1", verifier = "ver-1", authorization = authorization)

            assertEquals("tok-3", credential.token)
            val sent =
                server
                    .takeRequest()
                    .body
                    ?.utf8()
                    .orEmpty()
            assertTrue("the verifier is what proves this is the same client: $sent", sent.contains("ver-1"))
            assertTrue(sent.contains("grant_type=authorization_code"))
        }

    @Test
    fun `the account is read with the token, which is what says whose it is`() =
        runTest {
            answer("""{"id":812,"display_name":"A Reader","email":"reader@example.org"}""")

            val account = auth.account(Credential("tok-4"))

            assertEquals(812L, account.id)
            assertEquals("A Reader", account.displayName)
            assertEquals("Bearer tok-4", server.takeRequest().headers["Authorization"])
        }

    @Test
    fun `a refused token is an error rather than an account of nobody`() {
        answer("""{"error":"unauthorized"}""", code = 401)

        val refusal =
            assertThrows(WikiApiError.Status::class.java) {
                runTest { auth.account(Credential("stale")) }
            }
        assertEquals(401, refusal.code)
    }

    @Test
    fun `a new name is sent under the credential, and the account comes back wearing it`() =
        runTest {
            answer("""{"id":812,"display_name":"A Renamed Reader","email":"reader@example.org"}""")

            val renamed = auth.rename(to = "A Renamed Reader", credential = Credential("tok-6"))

            assertEquals("A Renamed Reader", renamed.displayName)
            val asked = server.takeRequest()
            assertEquals("/api/me", Uri.parse(asked.url.toString()).path)
            assertEquals("an account replaced instead of patched loses everything not sent", "PATCH", asked.method)
            assertEquals("Bearer tok-6", asked.headers["Authorization"])
            assertTrue(
                asked.body
                    ?.utf8()
                    .orEmpty()
                    .contains("A Renamed Reader"),
            )
        }

    @Test
    fun `signing out revokes the token rather than only forgetting it`() =
        runTest {
            answer("", code = 204)

            auth.signOut(Credential("tok-5"))

            val asked = server.takeRequest()
            assertEquals("/api/auth/signout", Uri.parse(asked.url.toString()).path)
            assertEquals("Bearer tok-5", asked.headers["Authorization"])
        }
}
