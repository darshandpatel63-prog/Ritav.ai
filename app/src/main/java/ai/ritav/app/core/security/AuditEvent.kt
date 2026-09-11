package ai.ritav.app.core.security

data class AuditEvent(
    val timestampEpochMillis: Long,
    val eventType: String,
    val appId: String,
    val action: String,
    val allowed: Boolean,
    val reason: String
)
