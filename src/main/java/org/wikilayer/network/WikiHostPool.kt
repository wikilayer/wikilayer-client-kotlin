package org.wikilayer.network

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** The host and typed reason recorded for one failed attempt. */
data class WikiHostFailure(
    val host: String,
    val reason: Reason,
) {
    sealed interface Reason {
        data class Network(
            val kind: NetworkKind,
        ) : Reason

        data class Http(
            val status: Int,
        ) : Reason

        data class Browser(
            val description: String,
        ) : Reason
    }
}

/** The network condition that prevented a host from answering. */
enum class NetworkKind {
    TIMEOUT,
    DNS,
    TLS,
    CONNECTION,
    IO,
}

/** An ordered set of hosts that remembers the last successful selection. */
class WikiHostPool(
    primary: String,
    mirrors: List<String> = emptyList(),
    preferred: String? = null,
    private val failoverHttpStatuses: Set<Int> = setOf(UNAVAILABLE_FOR_LEGAL_REASONS),
    private val didSelect: (String) -> Unit = {},
) {
    private val hosts = (listOf(primary) + mirrors).distinct()
    private var selected = preferred?.takeIf(hosts::contains) ?: primary

    @Synchronized
    fun candidates(): List<String> = listOf(selected) + hosts.filterNot { it == selected }

    @Synchronized
    fun select(host: String) {
        if (selected == host) return
        selected = host
        didSelect(host)
    }

    fun shouldFailover(afterHttpStatus: Int): Boolean = afterHttpStatus in failoverHttpStatuses

    private companion object {
        const val UNAVAILABLE_FOR_LEGAL_REASONS = 451
    }
}

@Suppress("ThrowsCount")
internal suspend fun <T> onAvailableHost(
    pool: WikiHostPool,
    operation: suspend (String) -> T,
): T {
    val failures = mutableListOf<WikiHostFailure>()
    for (host in pool.candidates()) {
        try {
            return operation(host).also { pool.select(host) }
        } catch (status: WikiApiError.Status) {
            if (!pool.shouldFailover(status.code)) throw status
            failures += WikiHostFailure(host, WikiHostFailure.Reason.Http(status.code))
        } catch (api: WikiApiError) {
            throw api
        } catch (network: IOException) {
            if (network.saysTheCallerStopped()) throw network
            failures +=
                WikiHostFailure(
                    host,
                    WikiHostFailure.Reason.Network(network.kind()),
                )
        }
    }
    throw WikiApiError.Unreachable(failures)
}

@Suppress("ThrowsCount")
internal suspend fun <T> onSelectedHost(
    pool: WikiHostPool,
    operation: suspend (String) -> T,
): T {
    val host = pool.candidates().firstOrNull() ?: throw WikiApiError.Unreachable(emptyList())
    return try {
        operation(host)
    } catch (api: WikiApiError) {
        throw api
    } catch (network: IOException) {
        if (network.saysTheCallerStopped()) throw network
        throw WikiApiError.Unreachable(
            listOf(
                WikiHostFailure(
                    host,
                    WikiHostFailure.Reason.Network(network.kind()),
                ),
            ),
            network,
        )
    }
}

internal fun IOException.saysTheCallerStopped(): Boolean = message == "Canceled"

internal fun IOException.kind(): NetworkKind =
    when (this) {
        is SocketTimeoutException -> NetworkKind.TIMEOUT
        is UnknownHostException -> NetworkKind.DNS
        is SSLException -> NetworkKind.TLS
        is ConnectException -> NetworkKind.CONNECTION
        else -> NetworkKind.IO
    }
