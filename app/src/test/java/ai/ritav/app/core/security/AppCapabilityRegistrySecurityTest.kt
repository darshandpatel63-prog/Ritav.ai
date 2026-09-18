package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNull
import org.junit.Test

class AppCapabilityRegistrySecurityTest {
    private val certificate = "a".repeat(64)

    @Test fun trustedCertificateIsBoundToPackageMetadata() {
        val registry = AppCapabilityRegistry(
            listOf(
                AppCapabilitySpec(
                    packageName = "com.example.safe",
                    capability = Capability.APP_LAUNCH,
                    actions = setOf("open"),
                    riskTier = RiskTier.TIER_1_REVERSIBLE,
                    trustedCertificateSha256 = certificate
                )
            )
        )

        assertEquals(certificate, registry.trustedCertificateSha256("com.example.safe"))
        assertNull(registry.trustedCertificateSha256("com.example.unknown"))
    }

    @Test fun invalidCertificateDigestIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AppCapabilityRegistry(
                listOf(
                    AppCapabilitySpec(
                        packageName = "com.example.safe",
                        capability = Capability.APP_LAUNCH,
                        actions = setOf("open"),
                        riskTier = RiskTier.TIER_1_REVERSIBLE,
                        trustedCertificateSha256 = "not-a-digest"
                    )
                )
            )
        }
    }

    @Test fun conflictingCertificatePinsForOnePackageAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AppCapabilityRegistry(
                listOf(
                    AppCapabilitySpec(
                        packageName = "com.example.safe",
                        capability = Capability.APP_LAUNCH,
                        actions = setOf("open"),
                        riskTier = RiskTier.TIER_1_REVERSIBLE,
                        trustedCertificateSha256 = "a".repeat(64)
                    ),
                    AppCapabilitySpec(
                        packageName = "com.example.safe",
                        capability = Capability.TYPE_TEXT,
                        actions = setOf("type"),
                        riskTier = RiskTier.TIER_1_REVERSIBLE,
                        trustedCertificateSha256 = "b".repeat(64)
                    )
                )
            )
        }
    }
}
