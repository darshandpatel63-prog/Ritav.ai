package ai.ritav.app.core.security

data class AuditEvent(
    val timestampEpochMillis: Long,
    val eventType: String,
    val appId: String,
    val action: String,
    val capability: Capability,
    val riskTier: RiskTier,
    val authorizationLevel: AuthorizationLevel,
    val allowed: Boolean,
    val reason: String,
    val sessionId: String? = null,
    val containedSensitiveData: Boolean = false
)

interface AuditLog {
    fun append(event: AuditEvent)
    fun snapshot(): List<AuditEvent>
    fun clear()
}

class InMemoryAuditLog : AuditLog {
    private val events = mutableListOf<AuditEvent>()

    override fun append(event: AuditEvent) {
        synchronized(events) { events += event.copy(action = "[ACTION_ID]", reason = event.reason.take(200)) }
    }

    override fun snapshot(): List<AuditEvent> = synchronized(events) { events.toList() }
    override fun clear() = synchronized(events) { events.clear() }
}
