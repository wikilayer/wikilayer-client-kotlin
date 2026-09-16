package org.wikilayer.network.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class WikiSummary(
    val id: Long,
    val title: String,
    @field:JsonProperty("url_path") val urlPath: String,
    @field:JsonProperty("updated_at") val updatedAt: Instant,
)

data class WikiPage(
    val wikis: List<WikiSummary>,
    @field:JsonProperty("has_more") val hasMore: Boolean,
)

data class ResolvedAddress(
    @field:JsonProperty("wiki_id") val wikiId: Long,
    @field:JsonProperty("node_id") val nodeId: Long,
    val language: String = "",
    @field:JsonProperty("wiki_title") val wikiTitle: String = "",
    @field:JsonProperty("wiki_url_path") val wikiUrlPath: String = "",
)
