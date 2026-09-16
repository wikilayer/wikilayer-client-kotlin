package org.wikilayer.network

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64 as SystemBase64

class Pkce {
    val verifier: String = randomText()
    val state: String = randomText()

    val challenge: String
        get() = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))

    companion object {
        private const val RANDOM_BYTES = 32

        private fun randomText(): String {
            val bytes = ByteArray(RANDOM_BYTES)
            SecureRandom().nextBytes(bytes)
            return base64Url(bytes)
        }

        private fun base64Url(bytes: ByteArray): String =
            SystemBase64.encodeToString(
                bytes,
                SystemBase64.URL_SAFE or SystemBase64.NO_PADDING or SystemBase64.NO_WRAP,
            )
    }
}
