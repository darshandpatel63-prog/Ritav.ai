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

    @Test fun sensitiveAuditReasonIsSuppressedBeforeStorage() {
        val log = InMemoryAuditLog()
        log.append(AuditEvent(1L, null, null, AuditEventType.POLICY_DECISION, false, false, "OTP 123456"))

        val stored = log.readAll().single()
        assertFalse(stored.reason.contains("123456"))
        assertEquals("Audit reason contained sensitive information and was suppressed", stored.reason)
    }

    @Test fun transformedSensitiveAuditReasonIsSuppressedBeforeStorage() {
        val log = InMemoryAuditLog()
        log.append(AuditEvent(1L, null, null, AuditEventType.POLICY_DECISION, false, false, "O\u200bTP 123456"))

        val stored = log.readAll().single()
        assertFalse(stored.reason.contains("123456"))
        assertEquals("Audit reason contained sensitive information and was suppressed", stored.reason)
    }

    @Test fun oversizedSessionMetadataIsRejectedBeforeStorage() {
        val log = InMemoryAuditLog()
        val oversizedSessionId = "s".repeat(129)

        val rejected = runCatching {
            log.append(
                AuditEvent(
                    timestampEpochMillis = 1L,
                    sessionId = oversizedSessionId,
                    actionHash = null,
                    eventType = AuditEventType.POLICY_DECISION,
                    allowed = false,
                    verified = false,
                    reason = "Denied"
                )
            )
        }.isFailure

        assertTrue(rejected)
        assertTrue(log.readAll().isEmpty())
    }

    @Test fun inMemoryAuditRetainsNewestEventsWhenCountBoundIsExceeded() {
        val log = InMemoryAuditLog()
        repeat(MAX_AUDIT_EVENTS + 5) { index ->
            log.append(
                AuditEvent(
                    timestampEpochMillis = index.toLong(),
                    sessionId = null,
                    actionHash = null,
                    eventType = AuditEventType.POLICY_DECISION,
                    allowed = false,
                    verified = false,
                    reason = "Denied"
                )
            )
        }

        val events = log.readAll()
        assertEquals(MAX_AUDIT_EVENTS, events.size)
        assertEquals(5L, events.first().timestampEpochMillis)
        assertEquals((MAX_AUDIT_EVENTS + 4).toLong(), events.last().timestampEpochMillis)
    }

    @Test fun serializedRetentionKeepsNewestWholeEventsWithinStorageBound() {
        val events = (1L..3L).map { timestamp ->
            AuditEvent(
                timestampEpochMillis = timestamp,
                sessionId = null,
                actionHash = null,
                eventType = AuditEventType.POLICY_DECISION,
                allowed = false,
                verified = false,
                reason = "Denied"
            )
        }

        val retained = retainNewestAuditEvents(events) { 30_000 }

        assertEquals(2, retained.size)
        assertEquals(2L, retained.first().timestampEpochMillis)
        assertEquals(3L, retained.last().timestampEpochMillis)
    }

    @Test fun serializedRetentionNeverExceedsEventCountEvenWhenSizeAllowsMore() {
        val events = (1L..(MAX_AUDIT_EVENTS + 10).toLong()).map { timestamp ->
            AuditEvent(
                timestampEpochMillis = timestamp,
                sessionId = null,
                actionHash = null,
                eventType = AuditEventType.POLICY_DECISION,
                allowed = false,
                verified = false,
                reason = "Denied"
            )
        }

        val retained = retainNewestAuditEvents(events) { 1 }

        assertEquals(MAX_AUDIT_EVENTS, retained.size)
        assertEquals(11L, retained.first().timestampEpochMillis)
        assertEquals((MAX_AUDIT_EVENTS + 10).toLong(), retained.last().timestampEpochMillis)
    }

    @Test fun rejectedPipelineDecisionIsAuditedWithoutActionContent() {
        val log = InMemoryAuditLog()
        val plan = ActionPlan("demo", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, expectedState = "EDITED", sessionId = "s1")
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
