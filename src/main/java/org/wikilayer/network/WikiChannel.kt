package org.wikilayer.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource
import org.wikilayer.network.model.Credential
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

fun interface Signing {
    fun credentialNow(): Credential?
}

class WikiChannel(
    private val baseUrl: String,
    client: OkHttpClient,
    private val reader: Signing = Signing { null },
    private val firstRetryMillis: Long = FIRST_RETRY_MILLIS,
    private val longestRetryMillis: Long = LONGEST_RETRY_MILLIS,
    private val onFailure: (Long, Throwable) -> Unit = { _, _ -> },
) {
    private val listeningWithoutGivingUpOnAQuietConnection =
        client
            .newBuilder()
            .readTimeout(NO_READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()

    fun changes(inWiki: Long): Flow<Unit> =
        flow {
            var retry = firstRetryMillis
            while (true) {
                try {
                    listen(inWiki) { emit(Unit) }
                    retry = firstRetryMillis
                } catch (networkGone: IOException) {
                    dropped(inWiki, networkGone)
                } catch (clientAlreadyClosed: IllegalStateException) {
                    dropped(inWiki, clientAlreadyClosed)
                } catch (redirectThatIsNoAddress: IllegalArgumentException) {
                    dropped(inWiki, redirectThatIsNoAddress)
                }
                delay(retry)
                retry = min(retry * 2, longestRetryMillis)
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun dropped(
        wikiId: Long,
        cause: Exception,
    ) {
        currentCoroutineContext().ensureActive()
        onFailure(wikiId, cause)
    }

    private suspend fun listen(
        wikiId: Long,
        changed: suspend () -> Unit,
    ) {
        val signedAs = reader.credentialNow()
        val request =
            Request
                .Builder()
                .url("$baseUrl/api/wikis/$wikiId/events")
                .header("Accept", "text/event-stream")
                .apply { if (signedAs != null) header("Authorization", "Bearer ${signedAs.token}") }
                .build()

        val call = listeningWithoutGivingUpOnAQuietConnection.newCall(request)
        coroutineScope {
            val unparkTheBlockedReadOnCancel =
                launch {
                    try {
                        awaitCancellation()
                    } finally {
                        call.cancel()
                    }
                }
            try {
                call.execute().use { response ->
                    if (response.code != OPEN_AND_STREAMING) throw WikiApiError.Status(response.code)
                    read(response.body.source(), changed)
                }
            } finally {
                unparkTheBlockedReadOnCancel.cancel()
            }
        }
    }

    private suspend fun read(
        source: BufferedSource,
        changed: suspend () -> Unit,
    ) {
        while (!source.exhausted()) {
            currentCoroutineContext().ensureActive()
            if (source.readUtf8LineStrict() == CHANGED) changed()
        }
    }

    private companion object {
        const val OPEN_AND_STREAMING = 200
        const val CHANGED = "event: changed"
        const val NO_READ_TIMEOUT = 0L
        const val FIRST_RETRY_MILLIS = 2_000L
        const val LONGEST_RETRY_MILLIS = 60_000L
    }
}
