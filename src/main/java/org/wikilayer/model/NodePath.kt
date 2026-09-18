package org.wikilayer.model

/** A dot-separated ancestry path returned with a synchronized node. */
@JvmInline
value class NodePath(
    val text: String,
) {
    val labels: List<String> get() = text.split(".").filter { it.isNotEmpty() }

    val wiki: Long get() = idAt(0)

    val page: Long get() = idAt(1)

    val parent: Long get() = idAt(labels.size - 2)

    val depth: Int get() = maxOf(labels.size - 1, 0)

    /** Returns whether the path has [from] among its ancestors. */
    fun descends(from: Long): Boolean = labels.dropLast(1).contains(from.toString())

    /** Returns whether this path is [other] or one of its descendants. */
    fun sitsInside(other: NodePath): Boolean = text == other.text || text.startsWith("${other.text}.")

    private fun idAt(position: Int): Long {
        val labels = this.labels
        if (position < 0 || position >= labels.size) return NONE
        return labels[position].toLongOrNull() ?: NONE
    }

    companion object {
        const val NONE = 0L
    }
}
