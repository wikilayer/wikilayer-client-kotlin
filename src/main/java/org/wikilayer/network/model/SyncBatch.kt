package org.wikilayer.network.model

import com.fasterxml.jackson.annotation.JsonProperty

/** An ordered page of synchronized nodes and the cursor that follows it. */
data class SyncBatch(
    val nodes: List<SyncNode>,
    @field:JsonProperty("has_more") val hasMore: Boolean,
    @field:JsonProperty("next_cursor") val cursor: String? = null,
)
