package ai.ritav.app.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class SecureLocalStoreValidationTest {
    @Test fun acceptsMaximumPreferenceNameLength() {
        validateSecureLocalStoreName("n".repeat(MAX_SECURE_STORE_NAME_LENGTH))
    }

    @Test fun rejectsOversizedPreferenceName() {
        assertIllegalArgument {
            validateSecureLocalStoreName("n".repeat(MAX_SECURE_STORE_NAME_LENGTH + 1))
        }
    }

    @Test fun acceptsMaximumSecureValueBytes() {
        validateSecureLocalStoreValueSize(MAX_SECURE_STORE_VALUE_BYTES)
    }

    @Test fun rejectsValueAboveMaximumByteSize() {
        assertIllegalArgument {
            validateSecureLocalStoreValueSize(MAX_SECURE_STORE_VALUE_BYTES + 1)
        }
    }

    @Test fun valueBoundIsByteBasedForMultiByteUtf8Content() {
        val value = "é".repeat(MAX_SECURE_STORE_VALUE_BYTES / 2)
        assertEquals(MAX_SECURE_STORE_VALUE_BYTES, value.toByteArray(Charsets.UTF_8).size)
        validateSecureLocalStoreValueSize(value.toByteArray(Charsets.UTF_8).size)
        assertIllegalArgument {
            val oversized = value + "é"
            validateSecureLocalStoreValueSize(oversized.toByteArray(Charsets.UTF_8).size)
        }
    }

    @Test fun acceptsMaximumEncodedValueLength() {
        validateSecureLocalStoreEncodedSize(MAX_SECURE_STORE_ENCODED_LENGTH)
    }

    @Test fun rejectsOversizedEncodedValueLength() {
        assertIllegalArgument {
            validateSecureLocalStoreEncodedSize(MAX_SECURE_STORE_ENCODED_LENGTH + 1)
        }
    }

    @Test fun rejectsEmptyEncodedValue() {
        assertIllegalArgument {
            validateSecureLocalStoreEncodedSize(0)
        }
    }

    @Test fun rejectsNegativeSize() {
        assertIllegalArgument {
            validateSecureLocalStoreValueSize(-1)
        }
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected fail-closed validation behavior.
        }
    }
}
