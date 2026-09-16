package org.wikilayer.network

import org.wikilayer.network.model.Credential
import org.wikilayer.network.model.MyWikiPage

interface MyWikisReading {
    suspend fun myWikis(
        after: String?,
        credential: Credential,
        limit: Int,
    ): MyWikiPage
}
