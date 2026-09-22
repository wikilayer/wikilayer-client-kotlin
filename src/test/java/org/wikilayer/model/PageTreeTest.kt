package org.wikilayer.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PageTreeTest {
    data class Cases(
        val trees: List<Tree>,
        val shape: List<Shape>,
        val outline: List<Outline>,
        val neighbours: List<Neighbours>,
        val noNeighbours: List<Nowhere>,
        val ancestors: List<Ancestors>,
    )

    data class Tree(
        val name: String,
        val pages: List<Page>,
    )

    data class Page(
        val id: Long,
        val parent: Long,
        val title: String,
        val sort: String = "10",
        val home: Boolean = false,
    )

    data class Shape(
        @param:JsonProperty("case") val name: String,
        val tree: String,
        val roots: List<Long>,
        val children: List<Children>,
    )

    data class Children(
        val page: Long,
        val pages: List<Long>,
    )

    data class Outline(
        @param:JsonProperty("case") val name: String,
        val tree: String,
        val expanded: List<Long>,
        val rows: List<Row>,
    )

    data class Row(
        val page: Long,
        val depth: Int,
        val branches: Boolean,
    )

    data class Neighbours(
        @param:JsonProperty("case") val name: String,
        val tree: String,
        val page: Long,
        val previous: Long,
        val next: Long,
    )

    data class Nowhere(
        @param:JsonProperty("case") val name: String,
        val tree: String,
        val page: Long,
    )

    data class Ancestors(
        @param:JsonProperty("case") val name: String,
        val tree: String,
        val page: Long,
        val above: List<Long>,
    )

    private fun tree(name: String): List<PageBranch> {
        val held =
            requireNotNull(cases.trees.firstOrNull { it.name == name }) {
                "page_tree_tests.yaml has no tree named $name"
            }
        return held.pages
            .map {
                PageInTree(
                    id = it.id,
                    parentId = it.parent,
                    title = it.title,
                    sortKey = it.sort,
                    isHome = it.home,
                )
            }.pageTree()
    }

    private fun branch(
        id: Long,
        inTree: List<PageBranch>,
    ): PageBranch? =
        inTree.firstNotNullOfOrNull { branch ->
            if (branch.id == id) branch else branch(id, branch.children)
        }

    @Test
    fun `pages nest by their parents`() {
        for (expected in cases.shape) {
            val tree = tree(expected.tree)

            assertEquals(expected.name, expected.roots, tree.map { it.id })
            for (held in expected.children) {
                val branch = branch(held.page, tree)
                assertNotNull("${expected.name}: page ${held.page}", branch)
                assertEquals(
                    "${expected.name}: under ${held.page}",
                    held.pages,
                    branch?.children?.map { it.id },
                )
            }
        }
    }

    @Test
    fun `the tree reads as a list of rows`() {
        for (expected in cases.outline) {
            val rows = tree(expected.tree).outline(expected.expanded.toSet())

            assertEquals(expected.name, expected.rows.map { it.page }, rows.map { it.id })
            assertEquals(expected.name, expected.rows.map { it.depth }, rows.map { it.depth })
            assertEquals(expected.name, expected.rows.map { it.branches }, rows.map { it.hasChildren })
        }
    }

    @Test
    fun `a page has the page before it and the page after it`() {
        for (expected in cases.neighbours) {
            val found = tree(expected.tree).neighbours(of = expected.page)

            assertNotNull(expected.name, found)
            assertEquals(expected.name, expected.previous, found?.previous?.id)
            assertEquals(expected.name, expected.next, found?.next?.id)
        }
    }

    @Test
    fun `a page with nowhere to step says so`() {
        for (expected in cases.noNeighbours) {
            assertNull(expected.name, tree(expected.tree).neighbours(of = expected.page))
        }
    }

    @Test
    fun `a page names the pages above it`() {
        for (expected in cases.ancestors) {
            assertEquals(expected.name, expected.above, tree(expected.tree).ancestors(of = expected.page))
        }
    }

    @Test
    fun `a repeated id is not drawn twice`() {
        val rows = tree("twice over").outline(setOf(10))

        assertEquals("a duplicate would show as two rows sharing an id", listOf(10L, 11L), rows.map { it.id })
    }

    private companion object {
        val cases: Cases =
            ObjectMapper(YAMLFactory())
                .registerKotlinModule()
                .readValue(
                    requireNotNull(PageTreeTest::class.java.getResourceAsStream("/page_tree_tests.yaml")) {
                        "page_tree_tests.yaml is missing: run `make sync-yaml`"
                    },
                )
    }
}
