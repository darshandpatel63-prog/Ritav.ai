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

    @Test fun registryDefensivelyCopiesMutableActionSets() {
        val actions = mutableSetOf("open")
        val registry = AppCapabilityRegistry(
            listOf(
                AppCapabilitySpec(
                    packageName = "com.example.safe",
                    capability = Capability.APP_LAUNCH,
                    actions = actions,
                    riskTier = RiskTier.TIER_1_REVERSIBLE,
                    trustedCertificateSha256 = certificate
                )
            )
        )

        actions += "unexpected"
        assertEquals(true, registry.allows("com.example.safe", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE))
        assertEquals(false, registry.allows("com.example.safe", Capability.APP_LAUNCH, "unexpected", RiskTier.TIER_1_REVERSIBLE))
    }

    @Test fun oversizedPackageNameIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AppCapabilityRegistry(
                listOf(
                    AppCapabilitySpec(
                        packageName = "a.".repeat(128),
                        capability = Capability.APP_LAUNCH,
                        actions = setOf("open"),
                        riskTier = RiskTier.TIER_1_REVERSIBLE,
                        trustedCertificateSha256 = certificate
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
