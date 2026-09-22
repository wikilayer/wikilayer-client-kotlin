package org.wikilayer.model

/**
 * A page of a wiki, as the tree of pages is built from it.
 *
 * [parentId] is the node the page hangs from, which is the wiki itself for a page at the
 * top. [sortKey] is the server's order among siblings, carried on every `SyncNode`; the
 * tree reads it rather than trusting the order pages are handed in, so two apps storing
 * the same wiki draw the same tree.
 */
data class PageInTree(
    val id: Long,
    val parentId: Long,
    val title: String,
    val sortKey: String,
    val isHome: Boolean = false,
)

/** A page with the pages kept under it. */
data class PageBranch(
    val id: Long,
    val title: String,
    val children: List<PageBranch>,
)

/** A page named as somewhere to go, without the pages kept under it. */
data class PageStep(
    val id: Long,
    val title: String,
)

/** The page before and the page after, in the order the wiki is read. */
data class PageNeighbours(
    val previous: PageStep,
    val next: PageStep,
)

/** A line of the tree as it is drawn: how deep it sits and whether it hides pages. */
data class PageOutlineRow(
    val id: Long,
    val title: String,
    val depth: Int,
    val hasChildren: Boolean,
)

private val byCodePoint =
    Comparator<String> { left, right ->
        var here = 0
        var there = 0
        var verdict = 0
        while (verdict == 0 && here < left.length && there < right.length) {
            val one = left.codePointAt(here)
            val other = right.codePointAt(there)
            verdict = one.compareTo(other)
            here += Character.charCount(one)
            there += Character.charCount(other)
        }
        if (verdict != 0) verdict else (left.length - here).compareTo(right.length - there)
    }

/**
 * Nests the pages of one wiki, in one language, into the tree their author made.
 *
 * Siblings come out in the server's order, by `sortKey` and then by id. The home page
 * leads, whatever it sorts as, because that is where a wiki is read from; hand it in with
 * the rest rather than filtering it out.
 *
 * Nothing handed in is dropped. A page whose parent is not among them is a root of its
 * own, and so is one whose parents form a cycle: a tree missing a page is worse than a
 * tree with a page at the wrong depth, because nothing on screen says it is missing. A
 * repeated id is kept once, since a node that changes mid-sync arrives in two batches and
 * a caller stitching them together has it twice.
 */
fun List<PageInTree>.pageTree(): List<PageBranch> {
    val pages =
        distinctBy { it.id }
            .sortedWith(
                compareByDescending<PageInTree> { it.isHome }
                    .thenBy(byCodePoint) { it.sortKey }
                    .thenBy { it.id },
            )

    val childrenOf = pages.filter { it.parentId != it.id }.groupBy { it.parentId }
    val placed = mutableSetOf<Long>()

    fun branch(page: PageInTree): PageBranch {
        placed.add(page.id)
        return PageBranch(
            id = page.id,
            title = page.title,
            children =
                childrenOf[page.id]
                    .orEmpty()
                    .filterNot { it.id in placed }
                    .map { branch(it) },
        )
    }

    val held = pages.map { it.id }.toSet()
    val tree = pages.filterNot { it.parentId in held }.map { branch(it) }.toMutableList()
    for (page in pages) {
        if (page.id !in placed) tree.add(branch(page))
    }
    return tree
}

/**
 * Reads the tree as one list, each row carrying how deep it sits.
 *
 * Only the branches named in [expanded] show what is under them, and a row says whether it
 * hides pages, so a closed branch can be told from a page with nothing under it. Which
 * branches stand open is the caller's to keep: the tree does not remember it between
 * drawings.
 */
fun List<PageBranch>.outline(expanded: Set<Long>): List<PageOutlineRow> = rows(expanded, depth = 0)

/**
 * The page before and after [of], walking the tree as a reader does.
 *
 * The walk closes into a ring, so the last page leads back to the first rather than to a
 * dead end. Fewer than three pages answer null: with two, both ways lead to the same page,
 * which reads as a control that does not work. A page the tree does not hold answers null
 * as well, and both answers mean the same thing to a caller — there is nowhere to step
 * from here.
 */
fun List<PageBranch>.neighbours(of: Long): PageNeighbours? {
    val reading = inReadingOrder()
    val here = reading.indexOfFirst { it.id == of }
    if (reading.size < FEWEST_PAGES_THAT_MAKE_A_WALK || here < 0) return null
    return PageNeighbours(
        previous = reading[(here + reading.size - 1) % reading.size],
        next = reading[(here + 1) % reading.size],
    )
}

/**
 * The pages above [of], outermost first, which have to stand open for it to show. A page at
 * the top, and a page the tree does not hold, have none.
 */
fun List<PageBranch>.ancestors(of: Long): List<Long> =
    firstNotNullOfOrNull { branch ->
        val deeper = branch.children.ancestors(of)
        when {
            branch.id == of -> emptyList()
            deeper.isNotEmpty() || branch.children.any { it.id == of } -> listOf(branch.id) + deeper
            else -> null
        }
    } ?: emptyList()

private fun List<PageBranch>.rows(
    expanded: Set<Long>,
    depth: Int,
): List<PageOutlineRow> =
    flatMap { branch ->
        val row =
            PageOutlineRow(
                id = branch.id,
                title = branch.title,
                depth = depth,
                hasChildren = branch.children.isNotEmpty(),
            )
        if (branch.id !in expanded) {
            listOf(row)
        } else {
            listOf(row) + branch.children.rows(expanded, depth + 1)
        }
    }

private fun List<PageBranch>.inReadingOrder(): List<PageStep> =
    flatMap { listOf(PageStep(id = it.id, title = it.title)) + it.children.inReadingOrder() }

private const val FEWEST_PAGES_THAT_MAKE_A_WALK = 3
