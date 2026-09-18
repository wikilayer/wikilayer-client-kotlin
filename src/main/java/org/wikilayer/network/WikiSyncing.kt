package org.wikilayer.network

import org.wikilayer.network.model.Credential
import org.wikilayer.network.model.SyncBatch

/** Fetches cursor-based pages of synchronized wiki nodes. */
interface WikiSyncing {
    suspend fun sync(
        wikiId: Long,
        after: String?,
        limit: Int,
        credential: Credential? = null,
    ): SyncBatch
}
