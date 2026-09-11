package ai.ritav.app.core.security

data class CapabilityGrant(
    val appId: String,
    val capability: Capability,
    val action: String,
    val sessionId: String? = null,
    val enabled: Boolean = true
)

interface PermissionStore {
    fun isGranted(appId: String, capability: Capability, action: String, sessionId: String?): Boolean
}

class InMemoryPermissionStore(grants: Set<CapabilityGrant> = emptySet()) : PermissionStore {
    private val grants = grants.toMutableSet()

    override fun isGranted(appId: String, capability: Capability, action: String, sessionId: String?): Boolean =
        grants.any { grant ->
            grant.enabled &&
                grant.appId == appId &&
                grant.capability == capability &&
                grant.action == action &&
                (grant.sessionId == null || grant.sessionId == sessionId)
        }

    fun grant(grant: CapabilityGrant) { grants.add(grant) }
    fun revoke(grant: CapabilityGrant) { grants.remove(grant) }
}
