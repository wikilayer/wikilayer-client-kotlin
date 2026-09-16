package org.wikilayer.network

import java.io.IOException

data class WikiHostFailure(
    val host: String,
    val reason: Reason,
) {
    sealed interface Reason {
        data class Network(
            val description: String,
        ) : Reason

        data class Http(
            val status: Int,
        ) : Reason

        data class Browser(
            val description: String,
        ) : Reason
    }
}

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
            failures +=
                WikiHostFailure(
                    host,
                    WikiHostFailure.Reason.Network(network.message ?: network.javaClass.simpleName),
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
        throw WikiApiError.Unreachable(
            listOf(
                WikiHostFailure(
                    host,
                    WikiHostFailure.Reason.Network(network.message ?: network.javaClass.simpleName),
                ),
            ),
            network,
        )
    }
}
