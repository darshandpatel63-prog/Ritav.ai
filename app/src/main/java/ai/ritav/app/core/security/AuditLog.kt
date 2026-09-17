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

internal const val MAX_AUDIT_EVENTS = 128
internal const val MAX_AUDIT_STORAGE_CHARS = 64_000

/** In-memory implementation for unit tests. */
class InMemoryAuditLog : AuditLog {
    private val events = mutableListOf<AuditEvent>()

    @Synchronized
    override fun append(event: AuditEvent) {
        events += validate(event)
        while (events.size > MAX_AUDIT_EVENTS) events.removeAt(0)
    }

    @Synchronized
    override fun readAll(): List<AuditEvent> = events.toList()

    @Synchronized
    override fun clear() = events.clear()

    private companion object {
        const val UNSAFE_REASON = "Audit reason contained sensitive information and was suppressed"
    }

    private fun validate(event: AuditEvent): AuditEvent = sanitizeAndValidate(event, UNSAFE_REASON)
}

/**
 * Persistent audit implementation backed by SecureLocalStore (AES-GCM and
 * Android Keystore). Only security metadata is persisted; secrets and content
 * bodies must never be supplied to this API.
 *
 * Retention is bounded by both event count and encoded storage size so repeated
 * audit activity cannot grow the persistent security-state value without bound.
 */
class SecureAuditLog(private val store: SecureLocalStore) : AuditLog {
    @Synchronized
    override fun append(event: AuditEvent) {
        val safe = validate(event)
        val existing = store.getString(STORAGE_KEY).orEmpty()
        val existingEvents = decodeAll(existing)
        val retained = retainNewestAuditEvents(existingEvents + safe) { auditEvent ->
            encode(auditEvent).length
        }
        val updated = retained.joinToString("\n", transform = ::encode)
        if (updated.isEmpty()) {
            store.remove(STORAGE_KEY)
        } else {
            store.putString(STORAGE_KEY, updated)
        }
    }

    @Synchronized
    override fun readAll(): List<AuditEvent> =
        decodeAll(store.getString(STORAGE_KEY).orEmpty())

    @Synchronized
    override fun clear() = store.remove(STORAGE_KEY)

    private fun decodeAll(raw: String): List<AuditEvent> {
        if (raw.isEmpty()) return emptyList()
        val boundedRaw = raw.takeLast(MAX_AUDIT_STORAGE_CHARS)
        return boundedRaw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { decode(it) }
            .toList()
            .takeLast(MAX_AUDIT_EVENTS)
    }

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

    private fun validate(event: AuditEvent): AuditEvent = sanitizeAndValidate(event, UNSAFE_REASON)

    private fun encodeField(value: String): String =
        android.util.Base64.encodeToString(value.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)

    private fun decodeField(value: String): String =
        String(android.util.Base64.decode(value, android.util.Base64.NO_WRAP), Charsets.UTF_8)

    private companion object {
        const val STORAGE_KEY = "security_audit_v1"
        const val MAX_SESSION_ID_LENGTH = 128
        const val UNSAFE_REASON = "Audit reason contained sensitive information and was suppressed"
    }
}

internal fun retainNewestAuditEvents(
    events: List<AuditEvent>,
    encodedLength: (AuditEvent) -> Int
): List<AuditEvent> {
    if (events.isEmpty()) return emptyList()
    val retained = ArrayDeque<AuditEvent>()
    var totalChars = 0

    for (event in events.asReversed()) {
        if (retained.size >= MAX_AUDIT_EVENTS) break
        val eventChars = encodedLength(event)
        val separatorChars = if (retained.isEmpty()) 0 else 1
        if (totalChars + eventChars + separatorChars > MAX_AUDIT_STORAGE_CHARS) break
        retained.addFirst(event)
        totalChars += eventChars + separatorChars
    }
    return retained.toList()
}

private fun sanitizeAndValidate(event: AuditEvent, unsafeReason: String): AuditEvent {
    require(event.timestampEpochMillis >= 0) { "Invalid audit timestamp" }
    require(event.sessionId?.length ?: 0 <= 128) { "Invalid session id" }
    require(event.actionHash?.length ?: 0 <= 128) { "Invalid action hash" }
    require(event.reason.length <= 512) { "Audit reason is too long" }
    require(!event.reason.contains('\n') && !event.reason.contains('\r')) {
        "Audit reason must be single-line"
    }

    val inspectedReason = SensitiveInformationFirewall().inspect(event.reason)
    val safeReason = if (inspectedReason.allowed) inspectedReason.redactedText.trim() else unsafeReason
    return event.copy(reason = safeReason)
}
