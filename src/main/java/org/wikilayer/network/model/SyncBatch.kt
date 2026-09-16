package org.wikilayer.network.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SyncBatch(
    val nodes: List<SyncNode>,
    @field:JsonProperty("has_more") val hasMore: Boolean,
    @field:JsonProperty("next_cursor") val cursor: String? = null,
)
