package org.wikilayer.network.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/** One entry in the public wiki directory. */
data class WikiSummary(
    val id: Long,
    val title: String,
    @field:JsonProperty("url_path") val urlPath: String,
    @field:JsonProperty("icon_url") val iconUrl: String? = null,
    @field:JsonProperty("pages_tree") val pagesTree: Boolean = false,
    @field:JsonProperty("updated_at") val updatedAt: Instant,
)

/** An offset-based page of public directory entries. */
data class WikiPage(
    val wikis: List<WikiSummary>,
    @field:JsonProperty("has_more") val hasMore: Boolean,
)

/** The wiki and node identified by a Wikilayer URL. */
data class ResolvedAddress(
    @field:JsonProperty("wiki_id") val wikiId: Long,
    @field:JsonProperty("node_id") val nodeId: Long,
    val language: String = "",
    @field:JsonProperty("wiki_title") val wikiTitle: String = "",
    @field:JsonProperty("wiki_url_path") val wikiUrlPath: String = "",
    @field:JsonProperty("wiki_icon_url") val wikiIconUrl: String? = null,
    @field:JsonProperty("wiki_pages_tree") val wikiPagesTree: Boolean = false,
)
