package ai.ritav.app.core.security

import ai.ritav.app.core.storage.SecureLocalStore

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

/** In-memory implementation for unit tests. */
class InMemoryAuditLog : AuditLog {
    private val events = mutableListOf<AuditEvent>()

    @Synchronized
    override fun append(event: AuditEvent) {
        events += validate(event)
    }

    @Synchronized
    override fun readAll(): List<AuditEvent> = events.toList()

    @Synchronized
    override fun clear() = events.clear()
}

/**
 * Persistent audit implementation backed by SecureLocalStore (AES-GCM and
 * Android Keystore). Only security metadata is persisted; secrets and content
 * bodies must never be supplied to this API.
 */
class SecureAuditLog(private val store: SecureLocalStore) : AuditLog {
    @Synchronized
    override fun append(event: AuditEvent) {
        val safe = validate(event)
        val encoded = encode(safe)
        val existing = store.getString(STORAGE_KEY).orEmpty()
        val updated = if (existing.isEmpty()) encoded else "$existing\n$encoded"
        store.putString(STORAGE_KEY, updated)
    }

    @Synchronized
    override fun readAll(): List<AuditEvent> {
        return store.getString(STORAGE_KEY).orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { decode(it) }
            .toList()
    }

    @Synchronized
    override fun clear() = store.remove(STORAGE_KEY)

    private fun encode(event: AuditEvent): String = listOf(
        event.timestampEpochMillis.toString(),
        encodeField(event.sessionId.orEmpty()),
        encodeField(event.actionHash.orEmpty()),
        event.eventType.name,
        event.allowed.toString(),
        event.verified.toString(),
        encodeField(event.reason)
    ).joinToString("|")

    private fun decode(encoded: String): AuditEvent? = runCatching {
        val parts = encoded.split('|')
        require(parts.size == 7)
        AuditEvent(
            timestampEpochMillis = parts[0].toLong(),
            sessionId = decodeField(parts[1]).ifEmpty { null },
            actionHash = decodeField(parts[2]).ifEmpty { null },
            eventType = AuditEventType.valueOf(parts[3]),
            allowed = parts[4].toBooleanStrict(),
            verified = parts[5].toBooleanStrict(),
            reason = decodeField(parts[6])
        )
    }.getOrNull()?.let { runCatching { validate(it) }.getOrNull() }

    private fun validate(event: AuditEvent): AuditEvent {
        require(event.timestampEpochMillis >= 0) { "Invalid audit timestamp" }
        require(event.actionHash?.length ?: 0 <= MAX_HASH_LENGTH) { "Invalid action hash" }
        require(event.reason.length <= MAX_REASON_LENGTH) { "Audit reason is too long" }
        require(!event.reason.contains('\n') && !event.reason.contains('\r')) {
            "Audit reason must be single-line"
        }
        return event.copy(reason = event.reason.trim())
    }

    private fun encodeField(value: String): String =
        android.util.Base64.encodeToString(value.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)

    private fun decodeField(value: String): String =
        String(android.util.Base64.decode(value, android.util.Base64.NO_WRAP), Charsets.UTF_8)

    private companion object {
        const val STORAGE_KEY = "security_audit_v1"
        const val MAX_REASON_LENGTH = 512
        const val MAX_HASH_LENGTH = 128
    }
}
