package org.wikilayer.network

import android.net.Uri
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.wikilayer.network.model.Credential
import org.wikilayer.network.model.MyWikiPage
import org.wikilayer.network.model.ResolvedAddress
import org.wikilayer.network.model.SyncBatch
import org.wikilayer.network.model.WikiPage
import java.io.IOException

sealed class WikiApiError(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause) {
    data class Status(
        val code: Int,
    ) : WikiApiError("the server answered $code")

    class Malformed(
        cause: Throwable,
    ) : WikiApiError("the answer could not be read: $cause", cause)

    class Unreachable(
        val failures: List<WikiHostFailure>,
        cause: Throwable? = null,
    ) : WikiApiError("none of the configured wiki hosts could be reached", cause)
}

class WikiApi(
    private val hosts: WikiHostPool,
    client: OkHttpClient,
    mapper: ObjectMapper,
    private val syncPageSize: Int = 500,
    private val directoryPageSize: Int = 50,
) : WikiSyncing,
    MyWikisReading {
    constructor(
        baseUrl: String,
        client: OkHttpClient,
        mapper: ObjectMapper,
        syncPageSize: Int = 500,
        directoryPageSize: Int = 50,
    ) : this(WikiHostPool(baseUrl), client, mapper, syncPageSize, directoryPageSize)

    val site: String get() = hosts.candidates().first()

    private val transport = JsonTransport(client, mapper)

    override suspend fun sync(
        wikiId: Long,
        after: String?,
        limit: Int,
        credential: Credential?,
    ): SyncBatch =
        onAvailableHost(hosts) { host ->
            fetch<SyncBatch>(
                address(host, "api/wikis/$wikiId/sync") {
                    appendQueryParameter("limit", limit.toString())
                    if (after != null) appendQueryParameter("cursor", after)
                },
                credential,
            )
        }

    suspend fun sync(
        wikiId: Long,
        after: String?,
        credential: Credential? = null,
    ): SyncBatch = sync(wikiId, after, syncPageSize, credential)

    suspend fun wikis(
        matching: String = "",
        limit: Int = directoryPageSize,
        offset: Int = 0,
    ): WikiPage =
        onAvailableHost(hosts) { host ->
            fetch(
                address(host, "api/wikis") {
                    appendQueryParameter("limit", limit.toString())
                    appendQueryParameter("offset", offset.toString())
                    if (matching.isNotEmpty()) appendQueryParameter("q", matching)
                },
            )
        }

    suspend fun resolve(
        link: String,
        credential: Credential? = null,
    ): ResolvedAddress =
        onAvailableHost(hosts) { host ->
            fetch(
                address(host, "api/resolve") { appendQueryParameter("url", link) },
                credential,
            )
        }

    override suspend fun myWikis(
        after: String?,
        credential: Credential,
        limit: Int,
    ): MyWikiPage =
        onAvailableHost(hosts) { host ->
            fetch(
                address(host, "api/me/wikis") {
                    appendQueryParameter("limit", limit.toString())
                    if (after != null) appendQueryParameter("cursor", after)
                },
                credential,
            )
        }

    suspend fun myWikis(
        after: String?,
        credential: Credential,
    ): MyWikiPage = myWikis(after, credential, syncPageSize)

    private fun address(
        host: String,
        path: String,
        query: Uri.Builder.() -> Unit,
    ): String =
        Uri
            .parse(host)
            .buildUpon()
            .appendEncodedPath(path)
            .apply(query)
            .build()
            .toString()

    private suspend inline fun <reified T> fetch(
        url: String,
        credential: Credential? = null,
    ): T = transport.value(T::class.java, signed(url, credential))

    private fun signed(
        url: String,
        credential: Credential?,
    ): Request =
        Request
            .Builder()
            .url(url)
            .apply { if (credential != null) header("Authorization", "Bearer ${credential.token}") }
            .build()
}
