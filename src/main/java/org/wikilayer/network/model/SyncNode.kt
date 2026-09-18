package org.wikilayer.network.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.wikilayer.model.NodePath
import java.time.Instant

/** The structural role of a synchronized node. */
enum class NodeKind(
    val raw: String,
) {
    @JsonProperty("wiki")
    WIKI("wiki"),

    @JsonProperty("page")
    PAGE("page"),

    @JsonProperty("block")
    BLOCK("block"),
    ;

    companion object {
        fun from(raw: String): NodeKind = entries.firstOrNull { it.raw == raw } ?: BLOCK
    }
}

/** The complete synchronized representation of one wiki node. */
data class SyncNode(
    val id: Long,
    val path: String = "",
    val kind: NodeKind = NodeKind.BLOCK,
    @field:JsonProperty("special_role") val specialRole: String = "",
    val title: String = "",
    val markdown: String = "",
    val language: String = "",
    @field:JsonProperty("sort_key") val sortKey: String = "",
    @field:JsonProperty("translation_group") val translationGroup: Long = 0,
    @field:JsonProperty("changed_at") val changedAt: Instant,
    val deleted: Boolean = false,
) {
    val nodePath: NodePath get() = NodePath(path)

    val parentId: Long get() = nodePath.parent

    val pageId: Long get() = nodePath.page

    val depth: Int get() = nodePath.depth
}
