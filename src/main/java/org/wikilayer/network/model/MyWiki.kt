package org.wikilayer.network.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

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

data class MyWiki(
    val id: Long,
    val title: String = "",
    @field:JsonProperty("url_path") val urlPath: String = "",
    @field:JsonProperty("updated_at") val updatedAt: Instant? = null,
    val visibility: WikiVisibility? = null,
    val mine: Boolean = false,
    val removed: Boolean = false,
)

data class MyWikiPage(
    val wikis: List<MyWiki>,
    @field:JsonProperty("has_more") val hasMore: Boolean = false,
    @field:JsonProperty("next_cursor") val cursor: String? = null,
)
