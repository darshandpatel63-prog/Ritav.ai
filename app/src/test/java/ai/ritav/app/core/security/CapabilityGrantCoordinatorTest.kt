package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityGrantCoordinatorTest {
    private val certificate = "a".repeat(64)

    private fun registryFor(riskTier: RiskTier) = AppCapabilityRegistry(
        listOf(
            AppCapabilitySpec(
                packageName = "com.example.safe",
                capability = Capability.APP_LAUNCH,
                actions = setOf("open"),
                riskTier = riskTier,
                trustedCertificateSha256 = certificate
            )
        )
    )

    @Test
    fun tierTwoApprovalCreatesSessionBoundGrantOnlyAfterExplicitConfirmation() {
        val registry = registryFor(RiskTier.TIER_1_REVERSIBLE)
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = 1_000L
        )
        val store = InMemoryPermissionStore()
        val service = CapabilityGrantService(
            registry,
            store,
            gate,
            stop,
            sessionManager
        )
        val authService = ActionAuthorizationService(
            gate = gate,
            deviceAuthorization = StubDeviceAuthorizationGateway(),
            clockEpochMillis = { 1_002L },
            emergencyStop = stop
        )
        val coordinator = CapabilityGrantCoordinator(
            registry = registry,
            grantService = service,
            authorizationService = authService,
            identitySessionManager = sessionManager,
            clockEpochMillis = { 1_001L }
        )
        val candidate = coordinator.options().single()
        val plan = requireNotNull(coordinator.prepare(candidate, session))

        var callbackCount = 0
        var result: Boolean? = null
        coordinator.approveAndGrant(plan, session, userConfirmed = true) {
            callbackCount++
            result = it
        }

        assertEquals(1, callbackCount)
        assertEquals(true, result)
        assertTrue(
            store.isGranted(
                "com.example.safe",
                Capability.APP_LAUNCH,
                "open",
                session.id
            )
        )
        assertFalse(
            store.isGranted(
                "com.example.safe",
                Capability.APP_LAUNCH,
                "open",
                null
            )
        )
    }

    @Test
    fun missingConfirmationNeverIssuesOrConsumesGrantAuthorization() {
        val registry = registryFor(RiskTier.TIER_1_REVERSIBLE)
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = 1_000L
        )
        val store = InMemoryPermissionStore()
        val service = CapabilityGrantService(registry, store, gate, stop, sessionManager)
        val authService = ActionAuthorizationService(
            gate = gate,
            deviceAuthorization = StubDeviceAuthorizationGateway(),
            emergencyStop = stop
        )
        val coordinator = CapabilityGrantCoordinator(
            registry,
            service,
            authService,
            sessionManager,
            clockEpochMillis = { 1_001L }
        )
        val plan = requireNotNull(coordinator.prepare(coordinator.options().single(), session))

        var result: Boolean? = null
        coordinator.approveAndGrant(plan, session, userConfirmed = false) {
            result = it
        }

        assertEquals(false, result)
        assertFalse(
            store.isGranted(
                "com.example.safe",
                Capability.APP_LAUNCH,
                "open",
                session.id
            )
        )
    }

    @Test
    fun forgedOrStalePlansAndSessionsAreRejected() {
        val registry = registryFor(RiskTier.TIER_1_REVERSIBLE)
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = 1_000L
        )
        val service = CapabilityGrantService(registry, InMemoryPermissionStore(), gate, stop, sessionManager)
        val authService = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(),
            emergencyStop = stop
        )
        val coordinator = CapabilityGrantCoordinator(
            registry,
            service,
            authService,
            sessionManager,
            clockEpochMillis = { 1_001L }
        )
        val plan = requireNotNull(coordinator.prepare(coordinator.options().single(), session))

        var wrongPlanResult: Boolean? = null
        coordinator.approveAndGrant(
            plan.copy(action = "grant:unexpected"),
            session,
            userConfirmed = true
        ) {
            wrongPlanResult = it
        }
        assertEquals(false, wrongPlanResult)

        stop.activate()
        stop.resetAfterExplicitUserConfirmation(true)

        var staleSessionResult: Boolean? = null
        coordinator.approveAndGrant(plan, session, userConfirmed = true) {
            staleSessionResult = it
        }
        assertEquals(false, staleSessionResult)
    }

    @Test
    fun tierThreeApprovalRequiresSuccessfulDeviceAuthentication() {
        val registry = registryFor(RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE)
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = 1_000L
        )
        val store = InMemoryPermissionStore()
        val service = CapabilityGrantService(registry, store, gate, stop, sessionManager)
        val authService = ActionAuthorizationService(
            gate = gate,
            deviceAuthorization = StubDeviceAuthorizationGateway(
                available = true,
                result = true
            ),
            clockEpochMillis = { 1_002L },
            emergencyStop = stop
        )
        val coordinator = CapabilityGrantCoordinator(
            registry,
            service,
            authService,
            sessionManager,
            clockEpochMillis = { 1_003L }
        )
        val plan = requireNotNull(coordinator.prepare(coordinator.options().single(), session))

        var result: Boolean? = null
        coordinator.approveAndGrant(plan, session, userConfirmed = true) {
            result = it
        }

        assertEquals(true, result)
        assertTrue(
            store.isGranted(
                "com.example.safe",
                Capability.APP_LAUNCH,
                "open",
                session.id
            )
        )
    }

    @Test
    fun tierThreeApprovalFailsClosedWhenDeviceAuthenticationFails() {
        val registry = registryFor(RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE)
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = 1_000L
        )
        val store = InMemoryPermissionStore()
        val service = CapabilityGrantService(registry, store, gate, stop, sessionManager)
        val authService = ActionAuthorizationService(
            gate = gate,
            deviceAuthorization = StubDeviceAuthorizationGateway(
                available = true,
                result = false
            ),
            emergencyStop = stop
        )
        val coordinator = CapabilityGrantCoordinator(
            registry,
            service,
            authService,
            sessionManager,
            clockEpochMillis = { 1_001L }
        )
        val plan = requireNotNull(coordinator.prepare(coordinator.options().single(), session))

        var result: Boolean? = null
        coordinator.approveAndGrant(plan, session, userConfirmed = true) {
            result = it
        }

        assertEquals(false, result)
        assertFalse(
            store.isGranted(
                "com.example.safe",
                Capability.APP_LAUNCH,
                "open",
                session.id
            )
        )
    }

    @Test
    fun emergencyStopBlocksApprovalAfterPlanPreparation() {
        val registry = registryFor(RiskTier.TIER_1_REVERSIBLE)
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = 1_000L
        )
        val store = InMemoryPermissionStore()
        val service = CapabilityGrantService(registry, store, gate, stop, sessionManager)
        val authService = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(),
            emergencyStop = stop
        )
        val coordinator = CapabilityGrantCoordinator(
            registry,
            service,
            authService,
            sessionManager,
            clockEpochMillis = { 1_001L }
        )
        val plan = requireNotNull(coordinator.prepare(coordinator.options().single(), session))

        stop.activate()

        var result: Boolean? = null
        coordinator.approveAndGrant(plan, session, userConfirmed = true) {
            result = it
        }

        assertEquals(false, result)
        assertFalse(
            store.isGranted(
                "com.example.safe",
                Capability.APP_LAUNCH,
                "open",
                session.id
            )
        )
    }
}
