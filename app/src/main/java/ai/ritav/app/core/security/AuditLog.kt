package ai.ritav.app.core.security

/** Security metadata safe for local audit. Never store message bodies or secrets. */
data class AuditEvent(
    val timestampEpochMillis: Long,
    val sessionId: String?,
    val actionHash: String?,
    val eventType: AuditEventType,
    val allowed: Boolean,
    val verified: Boolean,
    val reason: String
)

enum class AuditEventType {
    POLICY_DECISION,
    AUTHORIZATION,
    EXECUTION,
    VERIFICATION,
    EMERGENCY_STOP
}

interface AuditLog {
    fun append(event: AuditEvent)
    fun readAll(): List<AuditEvent>
    fun clear()
}

/** In-memory implementation for unit tests and early runtime wiring. */
class InMemoryAuditLog : AuditLog {
    private val events = mutableListOf<AuditEvent>()

    @Synchronized
    override fun append(event: AuditEvent) {
        require(event.reason.length <= MAX_REASON_LENGTH)
        events += event.copy(reason = event.reason.trim())
    }

    @Synchronized
    override fun readAll(): List<AuditEvent> = events.toList()

    @Synchronized
    override fun clear() = events.clear()

    private companion object {
        const val MAX_REASON_LENGTH = 512
    }
}
