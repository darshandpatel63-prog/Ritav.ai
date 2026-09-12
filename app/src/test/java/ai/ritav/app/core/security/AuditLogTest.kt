package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
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
        log.append(
            AuditEvent(1L, null, null, AuditEventType.POLICY_DECISION, false, false, "Denied")
        )
        log.clear()
        assertTrue(log.readAll().isEmpty())
    }
}
