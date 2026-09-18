package org.wikilayer.network

import org.wikilayer.network.model.Credential
import org.wikilayer.network.model.MyWikiPage

/** Reads cursor-based pages of wikis belonging to the authenticated account. */
interface MyWikisReading {
    suspend fun myWikis(
        after: String?,
        credential: Credential,
        limit: Int,
    ): MyWikiPage
}
