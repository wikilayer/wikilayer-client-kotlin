package org.wikilayer.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NodePathTest {
    @Test
    fun `the ids run from the wiki down, joined by dots`() {
        val block = NodePath("2982.4401.3903")

        assertEquals(2982L, block.wiki)
        assertEquals(4401L, block.parent)
        assertEquals(2, block.depth)
    }

    @Test
    fun `a wiki answers with itself and hangs from nothing`() {
        val root = NodePath("2982")

        assertEquals(2982L, root.wiki)
        assertEquals(NodePath.NONE, root.parent)
        assertEquals(0, root.depth)
    }

    @Test
    fun `a page hangs from what is above it, which need not be the wiki`() {
        assertEquals(2982L, NodePath("2982.4401").parent)
        assertEquals(1, NodePath("2982.4401").depth)
        assertEquals(
            "with pages under pages the node above is a page as often as it is the wiki",
            4401L,
            NodePath("2982.4401.4402").parent,
        )
    }

    @Test
    fun `a path that names no wiki answers with nothing rather than guessing`() {
        assertEquals(NodePath.NONE, NodePath("").wiki)
        assertEquals(NodePath.NONE, NodePath("wiki.page").wiki)
        assertEquals(0, NodePath("").depth)
    }

    @Test
    fun `a node descends from every id above it, and from no id beside it`() {
        val block = NodePath("1.10.11.12")

        assertTrue(block.descends(from = 1))
        assertTrue(block.descends(from = 10))
        assertTrue(block.descends(from = 11))
        assertFalse(block.descends(from = 12))
        assertFalse(block.descends(from = 2))
    }

    @Test
    fun `a node sits inside itself and inside every page above it`() {
        val page = NodePath("1.10")

        assertTrue(NodePath("1.10.11").sitsInside(page))
        assertTrue(page.sitsInside(page))
        assertFalse(NodePath("1.100").sitsInside(page))
        assertFalse(NodePath("1.20.21").sitsInside(page))
    }
}
