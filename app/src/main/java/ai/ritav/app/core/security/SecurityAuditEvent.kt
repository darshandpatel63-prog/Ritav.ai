package ai.ritav.app.core.security

/**
 * Minimal local audit record. Never put message bodies, passwords, OTPs, PINs,
 * CVVs, tokens, screen dumps, or other secrets into audit events.
 */
data class SecurityAuditEvent(
    val timestampEpochMillis: Long,
    val eventType: String,
    val appId: String? = null,
    val action: String? = null,
    val allowed: Boolean,
    val reason: String
)

interface SecurityAuditLog {
    fun append(event: SecurityAuditEvent)
    fun snapshot(): List<SecurityAuditEvent>
    fun clear()
}

class InMemorySecurityAuditLog : SecurityAuditLog {
    private val events = mutableListOf<SecurityAuditEvent>()

    @Synchronized
    override fun append(event: SecurityAuditEvent) {
        events += event
    }

    @Synchronized
    override fun snapshot(): List<SecurityAuditEvent> = events.toList()

    @Synchronized
    override fun clear() {
        events.clear()
    }
}
