package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityGrantServiceTest {
    private val registry = AppCapabilityRegistry(
        listOf(
            AppCapabilitySpec(
                packageName = "com.example.safe",
                capability = Capability.APP_LAUNCH,
                actions = setOf("open"),
                riskTier = RiskTier.TIER_1_REVERSIBLE
            )
        )
    )

    @Test
    fun grantRequiresExactAuthorizationAndCannotReplay() {
        val store = InMemoryPermissionStore()
        val gate = ActionAuthorizationGate(clockEpochMillis = { 2_001L })
        val service = CapabilityGrantService(registry, store, gate)
        val plan = service.createGrantPlan("com.example.safe", Capability.APP_LAUNCH, "open")

        assertTrue(plan != null)
        val validPlan = requireNotNull(plan)
        val token = gate.issue(validPlan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        assertTrue(service.grant(validPlan, token, 1_001L))
        assertTrue(store.isGranted("com.example.safe", Capability.APP_LAUNCH, "open", null))
        assertFalse(service.grant(validPlan, token, 1_002L))
    }

    @Test
    fun wrongPlanCannotConsumeGrantAuthorization() {
        val store = InMemoryPermissionStore()
        val gate = ActionAuthorizationGate(clockEpochMillis = { 2_001L })
        val service = CapabilityGrantService(registry, store, gate)
        val plan = requireNotNull(service.createGrantPlan("com.example.safe", Capability.APP_LAUNCH, "open"))
        val otherPlan = plan.copy(appId = "com.example.other")
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        assertFalse(service.grant(otherPlan, token, 1_001L))
        assertFalse(store.isGranted("com.example.other", Capability.APP_LAUNCH, "open", null))
    }

    @Test
    fun unknownAndFinancialCapabilitiesCannotCreateGrantPlans() {
        val store = InMemoryPermissionStore()
        val gate = ActionAuthorizationGate(clockEpochMillis = { 2_001L })
        val service = CapabilityGrantService(registry, store, gate)

        assertTrue(service.createGrantPlan("com.example.unknown", Capability.APP_LAUNCH, "open") == null)
        assertTrue(service.createGrantPlan("com.example.safe", Capability.FINANCIAL_ACTION, "pay") == null)
    }

    @Test
    fun grantAuthorizationRejectsInvalidClock() {
        val store = InMemoryPermissionStore()
        val gate = ActionAuthorizationGate(clockEpochMillis = { 2_001L })
        val service = CapabilityGrantService(registry, store, gate)
        val plan = requireNotNull(service.createGrantPlan("com.example.safe", Capability.APP_LAUNCH, "open"))
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        assertFalse(service.grant(plan, token, -1L))
        assertFalse(store.isGranted("com.example.safe", Capability.APP_LAUNCH, "open", null))
    }
    @Test
    fun lowerRiskGrantPlanCannotAuthorizeHigherRiskTarget() {
        val highRiskRegistry = AppCapabilityRegistry(
            listOf(
                AppCapabilitySpec(
                    packageName = "com.example.external",
                    capability = Capability.APP_LAUNCH,
                    actions = setOf("open"),
                    riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE
                )
            )
        )
        val store = InMemoryPermissionStore()
        val gate = ActionAuthorizationGate(clockEpochMillis = { 2_001L })
        val service = CapabilityGrantService(highRiskRegistry, store, gate)
        val forgedPlan = ActionPlan(
            appId = "com.example.external",
            capability = Capability.APP_LAUNCH,
            action = "grant:open",
            riskTier = RiskTier.TIER_2_CONTENT_MUTATION,
            expectedState = "CAPABILITY_GRANT"
        )
        val token = gate.issue(forgedPlan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        assertFalse(service.grant(forgedPlan, token, 1_001L))
        assertFalse(store.isGranted("com.example.external", Capability.APP_LAUNCH, "open", null))
    }

    @Test
    fun emergencyStopBlocksCapabilityGrantWithoutAffectingRevocationPath() {
        val store = InMemoryPermissionStore()
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop, clockEpochMillis = { 2_001L })
        val service = CapabilityGrantService(registry, store, gate)
        val plan = requireNotNull(service.createGrantPlan("com.example.safe", Capability.APP_LAUNCH, "open"))
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        stop.activate()
        assertFalse(service.grant(plan, token, 1_001L))
        assertFalse(store.isGranted("com.example.safe", Capability.APP_LAUNCH, "open", null))
        assertTrue(service.revoke(CapabilityGrant("com.example.safe", Capability.APP_LAUNCH, "open")))
    }

    @Test
    fun emergencyStopActivationCannotRacePastCapabilityGrantMutation() {
        val store = BlockingPermissionStore()
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop, clockEpochMillis = { 2_001L })
        val service = CapabilityGrantService(registry, store, gate, stop)
        val plan = requireNotNull(service.createGrantPlan("com.example.safe", Capability.APP_LAUNCH, "open"))
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        var grantResult = false
        val grantThread = Thread {
            grantResult = service.grant(plan, token, 1_001L)
        }
        grantThread.start()

        assertTrue(store.grantStarted.await(2, java.util.concurrent.TimeUnit.SECONDS))

        val stopAttempted = java.util.concurrent.CountDownLatch(1)
        val stopFinished = java.util.concurrent.CountDownLatch(1)
        val stopThread = Thread {
            stopAttempted.countDown()
            stop.activate()
            stopFinished.countDown()
        }
        stopThread.start()

        assertTrue(stopAttempted.await(2, java.util.concurrent.TimeUnit.SECONDS))
        assertFalse(stopFinished.await(250, java.util.concurrent.TimeUnit.MILLISECONDS))

        store.release.countDown()
        grantThread.join(2_000L)
        assertFalse(grantThread.isAlive)
        assertTrue(stopFinished.await(2, java.util.concurrent.TimeUnit.SECONDS))

        assertTrue(grantResult)
        assertTrue(stop.isActive())
        assertTrue(store.isGranted("com.example.safe", Capability.APP_LAUNCH, "open", null))
    }

    private class BlockingPermissionStore : MutablePermissionStore {
        val grantStarted = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        private var storedGrant: CapabilityGrant? = null

        @Synchronized
        override fun isGranted(appId: String, capability: Capability, action: String, sessionId: String?): Boolean =
            storedGrant?.let {
                it.enabled &&
                    it.appId == appId &&
                    it.capability == capability &&
                    it.action == action &&
                    it.sessionId == sessionId
            } == true

        override fun grant(grant: CapabilityGrant) {
            grantStarted.countDown()
            check(release.await(2, java.util.concurrent.TimeUnit.SECONDS))
            synchronized(this) {
                storedGrant = grant
            }
        }

        @Synchronized
        override fun revoke(grant: CapabilityGrant) {
            if (storedGrant == grant) storedGrant = null
        }
    }

    @Test
    fun sessionBoundGrantRequiresMatchingActiveIdentitySession() {
        val store = InMemoryPermissionStore()
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop, clockEpochMillis = { 2_001L })
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = 1_000L
        )
        val otherSession = sessionManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = 1_000L
        )
        val service = CapabilityGrantService(
            registry,
            store,
            gate,
            stop,
            sessionManager
        )
        val plan = requireNotNull(
            service.createGrantPlan(
                "com.example.safe",
                Capability.APP_LAUNCH,
                "open",
                sessionId = session.id
            )
        )
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        assertFalse(service.grant(plan, token, 1_001L, otherSession))
        assertFalse(store.isGranted("com.example.safe", Capability.APP_LAUNCH, "open", session.id))

        assertTrue(service.grant(plan, token, 1_001L, session))
        assertTrue(store.isGranted("com.example.safe", Capability.APP_LAUNCH, "open", session.id))
    }

    @Test
    fun staleSessionCannotAuthorizeGrantAfterEmergencyStopReset() {
        val store = InMemoryPermissionStore()
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop, clockEpochMillis = { 2_001L })
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = 1_000L
        )
        val service = CapabilityGrantService(
            registry,
            store,
            gate,
            stop,
            sessionManager
        )
        val plan = requireNotNull(
            service.createGrantPlan(
                "com.example.safe",
                Capability.APP_LAUNCH,
                "open",
                sessionId = session.id
            )
        )

        stop.activate()
        stop.resetAfterExplicitUserConfirmation(true)
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 2_000L)

        assertFalse(service.grant(plan, token, 2_001L, session))
        assertFalse(store.isGranted("com.example.safe", Capability.APP_LAUNCH, "open", session.id))
    }

    @Test
    fun financialCapabilityCannotBeGrantedEvenIfRegistryMetadataIsMisconfigured() {
        val financialRegistry = AppCapabilityRegistry(
            listOf(
                AppCapabilitySpec(
                    packageName = "com.example.finance",
                    capability = Capability.FINANCIAL_ACTION,
                    actions = setOf("pay"),
                    riskTier = RiskTier.TIER_1_REVERSIBLE
                )
            )
        )
        val service = CapabilityGrantService(
            financialRegistry,
            InMemoryPermissionStore(),
            ActionAuthorizationGate(clockEpochMillis = { 2_001L })
        )

        assertTrue(service.createGrantPlan("com.example.finance", Capability.FINANCIAL_ACTION, "pay") == null)
    }

}
