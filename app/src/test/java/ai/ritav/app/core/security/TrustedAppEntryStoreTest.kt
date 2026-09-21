package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedAppEntryStoreTest {
    private val certificate = "a".repeat(64)

    private fun validSpec(
        packageName: String = "com.example.safe",
        cert: String = certificate
    ) = AppCapabilitySpec(
        packageName = packageName,
        capability = Capability.APP_LAUNCH,
        actions = setOf("open"),
        riskTier = RiskTier.TIER_1_REVERSIBLE,
        sensitiveContentBlocked = true,
        financialCategory = false,
        trustedCertificateSha256 = cert
    )

    @Test
    fun unconfiguredStoreIsEmpty() {
        var raw: String? = null
        val store = SecureTrustedAppEntryStore(
            readRaw = { raw },
            writeRaw = { raw = it },
            removeRaw = { raw = null }
        )

        assertTrue(store.snapshot() is TrustedAppEntrySnapshot.Unconfigured)
        assertFalse(store.remove(validSpec()))
    }

    @Test
    fun validEntryRoundTripsWithDeterministicSerializedShape() {
        var raw: String? = null
        val store = SecureTrustedAppEntryStore(
            readRaw = { raw },
            writeRaw = { raw = it },
            removeRaw = { raw = null }
        )

        assertTrue(store.add(validSpec()))
        assertEquals(
            "com.example.safe|APP_LAUNCH|open|TIER_1_REVERSIBLE|${certificate}",
            raw
        )

        val snapshot = store.snapshot()
        assertTrue(snapshot is TrustedAppEntrySnapshot.Loaded)
        assertEquals(listOf(validSpec()), (snapshot as TrustedAppEntrySnapshot.Loaded).specs)

        assertTrue(store.remove(validSpec()))
        assertTrue(store.snapshot() is TrustedAppEntrySnapshot.Unconfigured)
    }

    @Test
    fun duplicatePackageAndInvalidSpecsFailClosed() {
        var raw: String? = null
        val store = SecureTrustedAppEntryStore(
            readRaw = { raw },
            writeRaw = { raw = it },
            removeRaw = { raw = null }
        )

        assertTrue(store.add(validSpec()))
        assertFalse(store.add(validSpec(cert = "b".repeat(64))))

        val financial = validSpec().copy(
            capability = Capability.FINANCIAL_ACTION,
            actions = setOf("pay"),
            riskTier = RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED,
            financialCategory = true
        )
        assertFalse(store.add(financial))
    }

    @Test
    fun corruptedStoredStateIsInvalidAndCannotBeOverwrittenByAdd() {
        var raw: String? = "corrupt"
        var writes = 0
        val store = SecureTrustedAppEntryStore(
            readRaw = { raw },
            writeRaw = { writes++; raw = it },
            removeRaw = { raw = null }
        )

        assertTrue(store.snapshot() is TrustedAppEntrySnapshot.Invalid)
        assertFalse(store.add(validSpec()))
        assertEquals(0, writes)
    }

    @Test
    fun storageWriteFailureFailsClosed() {
        var raw: String? = null
        val store = SecureTrustedAppEntryStore(
            readRaw = { raw },
            writeRaw = { throw IllegalStateException("write failed") },
            removeRaw = { raw = null }
        )

        assertFalse(store.add(validSpec()))
        assertTrue(store.snapshot() is TrustedAppEntrySnapshot.Unconfigured)
    }
}
