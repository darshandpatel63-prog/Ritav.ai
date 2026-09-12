package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditLogTest {
    @Test fun auditStoresOnlyStructuredSecurityMetadata() {
        val log = InMemoryAuditLog()
        log.append(
            AuditEvent(
                timestampEpochMillis = 1000L,
                sessionId = "session-1",
                actionHash = "hash",
                eventType = AuditEventType.EXECUTION,
                allowed = true,
                verified = true,
                reason = "Action verified"
            )
        )

        val events = log.readAll()
        assertEquals(1, events.size)
        assertEquals(AuditEventType.EXECUTION, events.first().eventType)
        assertTrue(events.first().verified)
    }

    @Test fun auditCanBeClearedByOwnerControlledStorageLayer() {
        val log = InMemoryAuditLog()
        log.append(AuditEvent(1L, null, null, AuditEventType.POLICY_DECISION, false, false, "Denied"))
        log.clear()
        assertTrue(log.readAll().isEmpty())
    }

    @Test fun rejectedPipelineDecisionIsAuditedWithoutActionContent() {
        val log = InMemoryAuditLog()
        val plan = ActionPlan("demo", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, "s1")
        val engine = PolicyEngine(InMemoryPermissionStore())
        val pipeline = SecurityExecutionPipeline(
            engine,
            ExecutionPolicyGate(engine),
            ActionAuthorizationGate(),
            auditLog = log
        )

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = ActionRequest("demo", "edit", RiskTier.TIER_2_CONTENT_MUTATION, Capability.UI_AUTOMATION, "s1"),
                plan = plan,
                nowEpochMillis = 1000L
            )
        )

        assertFalse(result.allowed)
        assertEquals(1, log.readAll().size)
        assertEquals(AuditEventType.POLICY_DECISION, log.readAll().first().eventType)
        assertEquals(plan.stableHash(), log.readAll().first().actionHash)
    }

    @Test fun approvedTier2DecisionAuditsAuthorizationAndPolicy() {
        val log = InMemoryAuditLog()
        val plan = ActionPlan("demo", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, "s1")
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "s1"))))
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), gate, auditLog = log)
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1000L)

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = ActionRequest("demo", "edit", RiskTier.TIER_2_CONTENT_MUTATION, Capability.UI_AUTOMATION, "s1", userExplicitlyRequested = true, authorizationLevel = AuthorizationLevel.USER_CONFIRMATION),
                plan = plan,
                authorizationToken = token,
                identitySession = SecuritySession("s1", IdentityLevel.OWNER_SIGNAL, 1000L, 61000L),
                nowEpochMillis = 1000L
            )
        )

        assertTrue(result.allowed)
        assertEquals(2, log.readAll().size)
        assertEquals(AuditEventType.AUTHORIZATION, log.readAll()[0].eventType)
        assertEquals(AuditEventType.POLICY_DECISION, log.readAll()[1].eventType)
    }
}
