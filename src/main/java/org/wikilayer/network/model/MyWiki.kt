package org.wikilayer.network.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/** The visibility reported for a wiki owned by the current account. */
enum class WikiVisibility(
    val raw: String,
) {
    @JsonProperty("public")
    PUBLIC("public"),

    @JsonProperty("private")
    PRIVATE("private"),
    ;

    companion object {
        fun from(raw: String): WikiVisibility? = entries.firstOrNull { it.raw == raw }
    }
}

/** One wiki in the authenticated account's cursor-based listing. */
data class MyWiki(
    val id: Long,
    val title: String = "",
    @field:JsonProperty("url_path") val urlPath: String = "",
    @field:JsonProperty("icon_url") val iconUrl: String? = null,
    @field:JsonProperty("pages_tree") val pagesTree: Boolean = false,
    @field:JsonProperty("updated_at") val updatedAt: Instant? = null,
    val visibility: WikiVisibility? = null,
    val mine: Boolean = false,
    val removed: Boolean = false,
)

/** A page of wikis belonging to the authenticated account. */
data class MyWikiPage(
    val wikis: List<MyWiki>,
    @field:JsonProperty("has_more") val hasMore: Boolean = false,
    @field:JsonProperty("next_cursor") val cursor: String? = null,
)
