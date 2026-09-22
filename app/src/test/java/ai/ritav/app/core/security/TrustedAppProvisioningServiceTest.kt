package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedAppProvisioningServiceTest {
    private val certificate = "a".repeat(64)

    private class FakeStore : TrustedAppEntryStore {
        val entries = mutableListOf<AppCapabilitySpec>()
        var failAdd = false

        override fun snapshot(): TrustedAppEntrySnapshot =
            if (entries.isEmpty()) TrustedAppEntrySnapshot.Unconfigured
            else TrustedAppEntrySnapshot.Loaded(entries.toList())

        override fun add(spec: AppCapabilitySpec): Boolean {
            if (failAdd || entries.any { it.packageName == spec.packageName }) return false
            entries += spec
            return true
        }

        override fun remove(spec: AppCapabilitySpec): Boolean = entries.remove(spec)
    }

    @Test
    fun exactEvidenceAndDeviceTokenPersistOneReviewedEntry() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val registry = AppCapabilityRegistry()
        val service = TrustedAppProvisioningService(registry, store, gate, stop, sessionManager)

        val plan = requireNotNull(
            service.createPlan("com.example.safe", certificate, 1, session, 1_001L)
        )
        val token = gate.issue(plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 1_002L)

        assertTrue(
            service.persist(
                plan,
                "com.example.safe",
                certificate,
                1,
                token,
                1_002L,
                session
            )
        )
        assertTrue(registry.isRegistered("com.example.safe"))
        assertEquals(1, store.entries.size)
    }

    @Test
    fun wrongEvidenceIsRejectedBeforeTokenConsumption() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val registry = AppCapabilityRegistry()
        val service = TrustedAppProvisioningService(registry, store, gate, stop, sessionManager)
        val plan = requireNotNull(
            service.createPlan("com.example.safe", certificate, 1, session, 1_001L)
        )
        val token = gate.issue(plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 1_002L)

        assertFalse(
            service.persist(
                plan,
                "com.example.safe",
                "b".repeat(64),
                1,
                token,
                1_002L,
                session
            )
        )

        assertTrue(
            service.persist(
                plan,
                "com.example.safe",
                certificate,
                1,
                token,
                1_002L,
                session
            )
        )
    }

    @Test
    fun certificateChangeCannotReuseOldPlanAuthorization() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val registry = AppCapabilityRegistry()
        val service = TrustedAppProvisioningService(registry, store, gate, stop, sessionManager)

        val certificateA = "a".repeat(64)
        val certificateB = "b".repeat(64)
        val planA = requireNotNull(
            service.createPlan("com.example.safe", certificateA, 1, session, 1_001L)
        )
        val planB = requireNotNull(
            service.createPlan("com.example.safe", certificateB, 1, session, 1_002L)
        )
        assertTrue(planA.stableHash() != planB.stableHash())

        val tokenA = gate.issue(planA, AuthorizationLevel.DEVICE_AUTHENTICATION, 1_003L)

        assertFalse(
            service.persist(
                planA,
                "com.example.safe",
                certificateA,
                1,
                tokenA,
                1_003L,
                session
            )
        )

        val tokenB = gate.issue(planB, AuthorizationLevel.DEVICE_AUTHENTICATION, 1_004L)
        assertTrue(
            service.persist(
                planB,
                "com.example.safe",
                certificateB,
                1,
                tokenB,
                1_004L,
                session
            )
        )
    }

    @Test
    fun malformedPackageNameFailsClosedWithoutRegisteringTrust() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val registry = AppCapabilityRegistry()
        val service = TrustedAppProvisioningService(registry, store, gate, stop, sessionManager)

        val plan = service.createPlan(
            packageName = "not a package",
            certificateSha256 = "a".repeat(64),
            signerCount = 1,
            identitySession = session,
            nowEpochMillis = 1_001L
        )

        assertFalse(plan != null)
        assertFalse(registry.isRegistered("not a package"))
        assertTrue(store.entries.isEmpty())
    }

    @Test
    fun multipleSignerAndEmergencyStopFailClosed() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val registry = AppCapabilityRegistry()
        val service = TrustedAppProvisioningService(registry, store, gate, stop, sessionManager)
        val plan = requireNotNull(
            service.createPlan("com.example.safe", certificate, 1, session, 1_001L)
        )
        val token = gate.issue(plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 1_002L)

        assertFalse(
            service.persist(
                plan,
                "com.example.safe",
                certificate,
                2,
                token,
                1_002L,
                session
            )
        )

        stop.activate()
        assertFalse(
            service.persist(
                plan,
                "com.example.safe",
                certificate,
                1,
                token,
                1_003L,
                session
            )
        )
        assertFalse(registry.isRegistered("com.example.safe"))
    }

    @Test
    fun storeFailureDoesNotActivateInMemoryTrust() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore().apply { failAdd = true }
        val registry = AppCapabilityRegistry()
        val service = TrustedAppProvisioningService(registry, store, gate, stop, sessionManager)
        val plan = requireNotNull(
            service.createPlan("com.example.safe", certificate, 1, session, 1_001L)
        )
        val token = gate.issue(plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 1_002L)

        assertFalse(
            service.persist(
                plan,
                "com.example.safe",
                certificate,
                1,
                token,
                1_002L,
                session
            )
        )
        assertFalse(registry.isRegistered("com.example.safe"))
    }
}
