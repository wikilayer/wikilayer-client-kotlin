package org.wikilayer.network

import android.net.Uri
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikilayer.network.model.Credential
import org.wikilayer.network.model.NodeKind
import org.wikilayer.network.model.WikiVisibility

@RunWith(RobolectricTestRunner::class)
class WikiApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: WikiApi

    @Before
    fun start() {
        server = MockWebServer()
        server.start()
        api =
            WikiApi(
                baseUrl = server.url("/").toString().trimEnd('/'),
                client = OkHttpClient(),
                mapper =
                    ObjectMapper()
                        .registerKotlinModule()
                        .registerModule(JavaTimeModule())
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false),
                syncPageSize = PAGE_SIZE,
                directoryPageSize = 50,
            )
    }

    @After
    fun stop() = server.close()

    private fun answer(
        body: String,
        code: Int = 200,
    ) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .body(body)
                .build(),
        )
    }

    private fun asked(): Uri = Uri.parse(server.takeRequest().url.toString())

    @Test
    fun `a first sync asks for the whole wiki and reads nodes out of the answer`() =
        runTest {
            answer(
                """
                {"nodes":[
                  {"id":2982,"path":"2982","kind":"wiki","title":"Guide","language":"en",
                   "changed_at":"2026-08-25T10:00:00.5Z"},
                  {"id":4401,"path":"2982.4401","kind":"page","title":"Agent rules","sort_key":"10",
                   "changed_at":"2026-08-25T10:00:01Z"}
                ],"has_more":true}
                """.trimIndent(),
            )

            val batch = api.sync(2982, after = null, limit = 500)

            assertEquals(2, batch.nodes.size)
            assertTrue(batch.hasMore)
            assertEquals(NodeKind.WIKI, batch.nodes[0].kind)
            assertEquals(2982L, batch.nodes[1].parentId)
            assertEquals(1, batch.nodes[1].depth)

            val url = asked()
            assertEquals("/api/wikis/2982/sync", url.path)
            assertNull("a first sync must not carry a cursor", url.getQueryParameter("cursor"))
        }

    @Test
    fun `every node says which page it belongs to, and the server is the one that knows`() =
        runTest {
            answer(
                """
                {"nodes":[
                  {"id":2982,"path":"2982","kind":"wiki","title":"Guide",
                   "changed_at":"2026-08-25T10:00:00Z"},
                  {"id":4401,"path":"2982.4401","kind":"page","title":"Agent rules","page_id":4401,
                   "changed_at":"2026-08-25T10:00:01Z"},
                  {"id":4402,"path":"2982.4401.4402","kind":"page","title":"Nested","page_id":4402,
                   "changed_at":"2026-08-25T10:00:02Z"},
                  {"id":4403,"path":"2982.4401.4402.4403","kind":"block","title":"Under the nested page",
                   "page_id":4402,"changed_at":"2026-08-25T10:00:03Z"}
                ],"has_more":false}
                """.trimIndent(),
            )

            val pages = api.sync(2982, after = null, limit = 500).nodes.associate { it.id to it.pageId }

            assertEquals(
                "a path carries ids and no kinds, so a reader that worked this out itself " +
                    "would stitch the block into the page above",
                4402L,
                pages[4403],
            )
            assertEquals("a page belongs to its own document", 4402L, pages[4402])
            assertEquals(4401L, pages[4401])
            assertEquals("the wiki belongs to no page", 0L, pages[2982])
        }

    @Test
    fun `a wiki says what it looks like on its own node, which is how every reader gets it`() =
        runTest {
            answer(
                """
                {"nodes":[
                  {"id":2982,"path":"2982","kind":"wiki","title":"Guide","pages_tree":true,
                   "icon_url":"https://wikilayer.org/s/icons/2982/abcdefgh.png",
                   "changed_at":"2026-08-25T10:00:00Z"},
                  {"id":4401,"path":"2982.4401","kind":"page","title":"Agent rules","page_id":4401,
                   "changed_at":"2026-08-25T10:00:01Z"}
                ],"has_more":false}
                """.trimIndent(),
            )

            val nodes = api.sync(2982, after = null, limit = 500).nodes

            assertEquals(
                "a reader who only follows a wiki is never listed it, " +
                    "so this is the one answer that reaches them",
                "https://wikilayer.org/s/icons/2982/abcdefgh.png",
                nodes[0].iconUrl,
            )
            assertTrue(nodes[0].pagesTree)
            assertNull("a page is not a wiki and carries none of this", nodes[1].iconUrl)
            assertFalse(nodes[1].pagesTree)
        }

    @Test
    fun `a wiki the reader does not hold yet is offered with its icon`() =
        runTest {
            answer(
                """
                {"wikis":[
                  {"id":2982,"title":"Guide","url_path":"/smee-again/guide",
                   "icon_url":"https://wikilayer.org/s/icons/2982/abcdefgh.png",
                   "updated_at":"2026-08-25T10:00:00Z"},
                  {"id":1025,"title":"Flat","url_path":"/smee-again/flat",
                   "updated_at":"2026-08-25T10:00:00Z"}
                ],"has_more":false}
                """.trimIndent(),
            )

            val wikis = api.wikis(matching = "guide").wikis

            assertEquals("https://wikilayer.org/s/icons/2982/abcdefgh.png", wikis[0].iconUrl)
            assertNull("a wiki with no icon says nothing about one", wikis[1].iconUrl)
        }

    @Test
    fun `resolving a link describes the wiki behind it the way the directory does`() =
        runTest {
            answer(
                """
                {"wiki_id":2982,"node_id":4401,"language":"en","wiki_title":"Guide",
                 "wiki_url_path":"/smee-again/guide",
                 "wiki_icon_url":"https://wikilayer.org/s/icons/2982/abcdefgh.png"}
                """.trimIndent(),
            )

            val found = api.resolve("https://wikilayer.org/smee-again/guide/4401")

            assertEquals(
                "a wiki followed from a link is stored from this answer alone, " +
                    "so what it leaves out the reader never gets",
                "https://wikilayer.org/s/icons/2982/abcdefgh.png",
                found.wikiIconUrl,
            )
        }

    @Test
    fun `the directory answers with wikis to follow, and says whether there are more`() =
        runTest {
            answer(
                """
                {"wikis":[
                  {"id":2982,"title":"Wikilayer authoring guide","url_path":"/smee-again/wikilayer-howto",
                   "updated_at":"2026-08-25T10:00:00.5Z"}
                ],"has_more":true}
                """.trimIndent(),
            )

            val page = api.wikis(matching = "guide")

            assertEquals(1, page.wikis.size)
            assertEquals(2982L, page.wikis[0].id)
            assertEquals("Wikilayer authoring guide", page.wikis[0].title)
            assertTrue(page.hasMore)

            val url = asked()
            assertEquals("/api/wikis", url.path)
            assertEquals("guide", url.getQueryParameter("q"))
        }

    @Test
    fun `a link from outside is handed to the server, which says what it points at`() =
        runTest {
            answer("""{"wiki_id":2982,"node_id":39340,"language":"en"}""")

            val found = api.resolve("https://wikilayer.org/smee-again/howto/4401#block-39340")

            assertEquals(2982L, found.wikiId)
            assertEquals(39340L, found.nodeId)
            assertEquals("en", found.language)

            val url = asked()
            assertEquals("/api/resolve", url.path)
            assertTrue(
                "the anchor is part of the address and has to travel",
                url.getQueryParameter("url").orEmpty().contains("block-39340"),
            )
        }

    @Test
    fun `an empty query asks for the directory whole, without an empty filter`() =
        runTest {
            answer("""{"wikis":[],"has_more":false}""")

            api.wikis()

            assertNull("an empty q would ask the server to match nothing", asked().getQueryParameter("q"))
        }

    @Test
    fun `a plus in a search reaches the server as a plus, not as two spaces`() =
        runTest {
            answer("""{"wikis":[],"has_more":false}""")

            api.wikis(matching = "C++")

            assertEquals(
                "the server reads a bare plus in a query as a space, so «C++» would arrive as «C  »",
                "C++",
                asked().getQueryParameter("q"),
            )
        }

    @Test
    fun `the cursor a wiki's sync handed over goes back to it as it came`() =
        runTest {
            answer(
                """
                {"nodes":[{"id":39340,"path":"2982.39339.39340","kind":"block","title":"Rules",
                           "changed_at":"2026-08-25T10:00:02.25Z"}],
                 "has_more":true,"next_cursor":"1756116002.250000|39340"}
                """.trimIndent(),
            )
            val cursor = api.sync(2982, after = null, limit = PAGE_SIZE).cursor
            asked()

            answer("""{"nodes":[],"has_more":false}""")
            api.sync(2982, after = cursor, limit = PAGE_SIZE)

            assertEquals("1756116002.250000|39340", asked().getQueryParameter("cursor"))
        }

    @Test
    fun `a wiki with more to come but no cursor leaves the client where it was`() =
        runTest {
            answer(
                """
                {"nodes":[{"id":7,"path":"1.7","kind":"page","title":"Ferries",
                           "changed_at":"2026-08-25T10:00:02.257843Z"}],"has_more":true}
                """.trimIndent(),
            )

            assertNull(
                "a client that invents a cursor of its own asks this server for the same page for ever",
                api.sync(1, after = null, limit = PAGE_SIZE).cursor,
            )
        }

    @Test
    fun `a deleted node arrives as an id and a flag, with no fields to speak of`() =
        runTest {
            answer("""{"nodes":[{"id":4036,"changed_at":"2026-08-25T11:00:00Z","deleted":true}],"has_more":false}""")

            val gone = api.sync(2982, after = null, limit = 500).nodes.single()

            assertTrue(gone.deleted)
            assertEquals(4036L, gone.id)
            assertTrue(gone.title.isEmpty())
        }

    @Test
    fun `a batch that leaves out has_more is the last one, not a broken answer`() =
        runTest {
            answer("""{"nodes":[]}""")

            val batch = api.sync(2982, after = null, limit = 500)

            assertFalse(batch.hasMore)
        }

    @Test
    fun `the reader's own wikis are asked for with their token, and say which are theirs`() =
        runTest {
            answer(
                """
                {"wikis":[
                  {"id":7,"title":"Ferries","url_path":"/a-reader/ferries","visibility":"private","mine":true},
                  {"id":9,"title":"Harbours","url_path":"/somebody/harbours","visibility":"public"},
                  {"id":11,"removed":true}
                ],"has_more":false,"next_cursor":"c-2"}
                """.trimIndent(),
            )

            val page = api.myWikis(after = "c-1", credential = Credential("tok"), limit = PAGE_SIZE)

            assertEquals(3, page.wikis.size)
            assertTrue("a wiki of the reader's own is marked as theirs", page.wikis[0].mine)
            assertEquals(WikiVisibility.PRIVATE, page.wikis[0].visibility)
            assertFalse("somebody else's wiki is not theirs", page.wikis[1].mine)
            assertTrue("a wiki that left carries nothing but its id", page.wikis[2].removed)
            assertEquals("c-2", page.cursor)

            val asked = server.takeRequest()
            assertEquals("/api/me/wikis", Uri.parse(asked.url.toString()).path)
            assertEquals("c-1", Uri.parse(asked.url.toString()).getQueryParameter("cursor"))
            assertEquals("Bearer tok", asked.headers["Authorization"])
        }

    @Test
    fun `a first read of the reader's own wikis carries no cursor`() =
        runTest {
            answer("""{"wikis":[],"has_more":false}""")

            api.myWikis(after = null, credential = Credential("tok"), limit = PAGE_SIZE)

            assertNull(Uri.parse(server.takeRequest().url.toString()).getQueryParameter("cursor"))
        }

    @Test
    fun `the cursor the server handed over goes back to it as it came`() =
        runTest {
            answer("""{"wikis":[],"has_more":true,"next_cursor":"where-the-set-got-to"}""")
            answer("""{"wikis":[],"has_more":false}""")

            val page = api.myWikis(after = null, credential = Credential("tok"), limit = PAGE_SIZE)
            api.myWikis(after = page.cursor, credential = Credential("tok"), limit = PAGE_SIZE)

            server.takeRequest()
            assertEquals("where-the-set-got-to", asked().getQueryParameter("cursor"))
        }

    @Test
    fun `a refusal is the server's answer about the credential, not something to hide`() {
        answer("""{"error":"not signed in"}""", code = 401)

        val failure =
            assertThrows(WikiApiError.Status::class.java) {
                runTest { api.myWikis(after = null, credential = Credential("tok"), limit = PAGE_SIZE) }
            }
        assertEquals(401, failure.code)
    }

    @Test
    fun `an answer outside 200 but still a success is read, as on the reference`() =
        runTest {
            answer("""{"nodes":[],"has_more":false}""", code = 202)

            assertTrue(api.sync(1, after = null, limit = PAGE_SIZE).nodes.isEmpty())
        }

    @Test
    fun `a refusal is reported as a refusal, not as an empty wiki`() {
        answer("not found", code = 404)

        val failure =
            assertThrows(WikiApiError.Status::class.java) {
                runTest { api.sync(1, after = null, limit = 500) }
            }
        assertEquals(404, failure.code)
    }

    @Test
    fun `an answer that is not a batch is refused rather than read as an empty one`() {
        answer("""{"items":[],"more":false}""")

        assertThrows(WikiApiError.Malformed::class.java) {
            runTest { api.sync(1, after = null, limit = 500) }
        }
    }

    @Test
    fun `a node without a timestamp is refused rather than dated to the epoch`() {
        answer("""{"nodes":[{"id":4401,"path":"1.4401","kind":"page"}],"has_more":false}""")

        assertThrows(WikiApiError.Malformed::class.java) {
            runTest { api.sync(1, after = null, limit = 500) }
        }
    }

    private companion object {
        const val PAGE_SIZE = 500
    }
}
