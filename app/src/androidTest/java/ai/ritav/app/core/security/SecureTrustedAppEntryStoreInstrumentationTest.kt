package ai.ritav.app.core.security

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureTrustedAppEntryStoreInstrumentationTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val certificate = "a".repeat(64)

    @Test
    fun reviewedTrustEntryPersistsEncryptedAtRest() {
        val localStore = ai.ritav.app.core.storage.SecureLocalStore(context)
        localStore.remove("trusted_app_entries_v1")
        val store = SecureTrustedAppEntryStore(localStore)
        val spec = AppCapabilitySpec(
            packageName = "com.example.safe",
            capability = Capability.APP_LAUNCH,
            actions = setOf("open"),
            riskTier = RiskTier.TIER_1_REVERSIBLE,
            sensitiveContentBlocked = true,
            financialCategory = false,
            trustedCertificateSha256 = certificate
        )

        try {
            assertTrue(store.add(spec))
            assertTrue(store.snapshot() is TrustedAppEntrySnapshot.Loaded)

            val rawStored = context
                .getSharedPreferences("ritav_secure_state", Context.MODE_PRIVATE)
                .getString("trusted_app_entries_v1", null)

            assertFalse(rawStored.isNullOrBlank())
            assertFalse(rawStored.orEmpty().contains("com.example.safe"))
            assertFalse(rawStored.orEmpty().contains(certificate))
            assertTrue(store.snapshot() is TrustedAppEntrySnapshot.Loaded)
        } finally {
            localStore.remove("trusted_app_entries_v1")
        }
    }
}
