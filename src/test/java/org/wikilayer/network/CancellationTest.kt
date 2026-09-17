package org.wikilayer.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class CancellationTest {
    private val pool = WikiHostPool(primary = "https://wiki.example")

    @Test
    fun `a call the reader stopped is not a host that could not be reached`() =
        runTest {
            val stopped = caught { onAvailableHost(pool) { throw IOException(CANCELLED) } }

            assertEquals(
                "wrapped as unreachable, a stop reads as a server that is down, and the caller " +
                    "that would have kept quiet reports it instead",
                CANCELLED,
                stopped?.message,
            )
        }

    @Test
    fun `a stopped call to the selected host is not a host that could not be reached`() =
        runTest {
            val stopped = caught { onSelectedHost(pool) { throw IOException(CANCELLED) } }

            assertEquals(CANCELLED, stopped?.message)
        }

    private suspend fun caught(operation: suspend () -> Unit): IOException? =
        try {
            operation()
            null
        } catch (failure: IOException) {
            failure
        }

    private companion object {
        const val CANCELLED = "Canceled"
    }
}
