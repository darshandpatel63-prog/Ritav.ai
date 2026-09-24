package ai.ritav.core.security.windows

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import platform.posix.getenv
import platform.posix.remove

class WindowsSecureLocalStoreTest {
    @Test
    fun roundTripAndOverwrite() {
        val directory = testDirectory()
        val store = WindowsSecureLocalStore(directory)

        store.putString("generation", "first")
        assertEquals("first", store.getString("generation"))

        store.putString("generation", "second")
        assertEquals("second", store.getString("generation"))

        store.remove("generation")
        assertNull(store.getString("generation"))
        cleanupDirectory(directory)
    }

    @Test
    fun oversizedValueFailsClosed() {
        val directory = testDirectory()
        val store = WindowsSecureLocalStore(directory)

        assertFailsWith<IllegalArgumentException> {
            store.putString("generation", "x".repeat(MAX_WINDOWS_SECURE_VALUE_BYTES + 1))
        }
        cleanupDirectory(directory)
    }

    @Test
    fun invalidKeyFailsClosed() {
        val directory = testDirectory()
        val store = WindowsSecureLocalStore(directory)

        assertFailsWith<IllegalArgumentException> {
            store.putString("../escape", "value")
        }
        cleanupDirectory(directory)
    }

    private fun testDirectory(): String {
        val temp = getenv("TEMP")?.toKString()
            ?: error("Windows TEMP is unavailable")
        return temp + "/ritav-windows-secure-" + Random.nextLong().toString()
    }

    private fun cleanupDirectory(directory: String) {
        // The test data file is keyed by the encoded form of "generation".
        remove(directory + "/67656e65726174696f6e.bin")
        remove(directory)
    }
}
