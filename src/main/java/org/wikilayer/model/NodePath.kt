package org.wikilayer.model

@JvmInline
value class NodePath(
    val text: String,
) {
    val labels: List<String> get() = text.split(".").filter { it.isNotEmpty() }

    val wiki: Long get() = idAt(0)

    val page: Long get() = idAt(1)

    val parent: Long get() = idAt(labels.size - 2)

    val depth: Int get() = maxOf(labels.size - 1, 0)

    fun descends(from: Long): Boolean = labels.dropLast(1).contains(from.toString())

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
