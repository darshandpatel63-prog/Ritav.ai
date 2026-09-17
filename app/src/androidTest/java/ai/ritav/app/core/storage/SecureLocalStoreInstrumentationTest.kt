package ai.ritav.app.core.storage

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureLocalStoreInstrumentationTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun roundTripUsesKeystoreBackedCiphertextAtRest() {
        val key = uniqueKey("roundtrip")
        val value = "Ritav instrumentation 🔐 UTF-8 payload"
        val store = SecureLocalStore(context)

        store.putString(key, value)

        val rawStored = context
            .getSharedPreferences("ritav_secure_state", Context.MODE_PRIVATE)
            .getString(key, null)

        assertNotNull(rawStored)
        assertNotEquals(value, rawStored)
        assertFalse(rawStored.orEmpty().contains(value))
        assertEquals(value, store.getString(key))

        store.remove(key)
    }

    @Test
    fun tamperedCiphertextFailsClosedWithoutPlaintextFallback() {
        val key = uniqueKey("tamper")
        val value = "tamper-sensitive-test-value"
        val store = SecureLocalStore(context)
        val preferences = context.getSharedPreferences("ritav_secure_state", Context.MODE_PRIVATE)

        try {
            store.putString(key, value)
            val original = preferences.getString(key, null)
            require(!original.isNullOrEmpty())
            val last = original.last()
            val replacement = if (last == 'A') 'B' else 'A'
            check(preferences.edit().putString(key, original.dropLast(1) + replacement).commit())

            assertThrows(Exception::class.java) {
                store.getString(key)
            }
        } finally {
            preferences.edit().remove(key).commit()
        }
    }

    private fun uniqueKey(prefix: String): String =
        "instrumentation_${prefix}_${System.nanoTime()}"
}
