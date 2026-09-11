package ai.ritav.app.core.security

import ai.ritav.app.core.storage.SecureLocalStore

/**
 * Durable permission store backed by SecureLocalStore.
 *
 * The serialized permission entries contain only policy metadata. No secrets,
 * message bodies, contact content, or screen data are stored here.
 */
class SecurePermissionStore(
    private val store: SecureLocalStore
) : PermissionStore {

    override fun isGranted(
        appId: String,
        capability: Capability,
        action: String,
        sessionId: String?
    ): Boolean = loadGrants().any { grant ->
        grant.enabled &&
            grant.appId == appId &&
            grant.capability == capability &&
            grant.action == action &&
            (grant.sessionId == null || grant.sessionId == sessionId)
    }

    @Synchronized
    fun grant(grant: CapabilityGrant) {
        require(grant.appId.isNotBlank())
        require(grant.action.isNotBlank())
        if (grant.capability == Capability.FINANCIAL_ACTION) return
        saveGrants((loadGrants() + grant.copy(enabled = true)).distinct())
    }

    @Synchronized
    fun revoke(grant: CapabilityGrant) {
        saveGrants(loadGrants().filterNot { it == grant })
    }

    @Synchronized
    fun revokeAllForApp(appId: String) {
        saveGrants(loadGrants().filterNot { it.appId == appId })
    }

    private fun loadGrants(): List<CapabilityGrant> {
        val raw = store.getString(STORAGE_KEY) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .mapNotNull { decodeGrant(it) }
            .toList()
    }

    private fun saveGrants(grants: List<CapabilityGrant>) {
        if (grants.isEmpty()) {
            store.remove(STORAGE_KEY)
            return
        }
        store.putString(STORAGE_KEY, grants.joinToString("\n") { encodeGrant(it) })
    }

    private fun encodeGrant(grant: CapabilityGrant): String = listOf(
        encodeField(grant.appId),
        grant.capability.name,
        encodeField(grant.action),
        encodeField(grant.sessionId ?: ""),
        grant.enabled.toString()
    ).joinToString("|")

    private fun decodeGrant(encoded: String): CapabilityGrant? = runCatching {
        val parts = encoded.split('|')
        require(parts.size == 5)
        CapabilityGrant(
            appId = decodeField(parts[0]),
            capability = Capability.valueOf(parts[1]),
            action = decodeField(parts[2]),
            sessionId = decodeField(parts[3]).ifEmpty { null },
            enabled = parts[4].toBooleanStrict()
        )
    }.getOrNull()?.takeUnless { it.capability == Capability.FINANCIAL_ACTION }

    private fun encodeField(value: String): String =
        android.util.Base64.encodeToString(value.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)

    private fun decodeField(value: String): String =
        String(android.util.Base64.decode(value, android.util.Base64.NO_WRAP), Charsets.UTF_8)

    private companion object {
        const val STORAGE_KEY = "permission_grants_v1"
    }
}
