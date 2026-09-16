package org.wikilayer.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class PkceTest {
    @Test
    fun `the challenge is what the server recomputes from the verifier`() {
        val secret = Pkce()

        val digest = MessageDigest.getInstance("SHA-256").digest(secret.verifier.toByteArray())
        val expected =
            Base64
                .getEncoder()
                .encodeToString(digest)
                .replace("+", "-")
                .replace("/", "_")
                .replace("=", "")

        assertEquals(expected, secret.challenge)
        assertFalse(
            "padding is not part of the base64url the server compares",
            secret.challenge.contains("="),
        )
    }

    @Test
    fun `no two sign-ins share a secret`() {
        val one = Pkce()
        val two = Pkce()

        assertNotEquals(one.verifier, two.verifier)
        assertNotEquals(one.state, two.state)
        assertTrue("a verifier shorter than this is guessable", one.verifier.length >= SHORTEST_VERIFIER)
    }

    private companion object {
        const val SHORTEST_VERIFIER = 43
    }
}
