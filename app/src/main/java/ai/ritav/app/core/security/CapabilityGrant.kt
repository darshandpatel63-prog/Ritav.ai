package ai.ritav.app.core.security

data class CapabilityGrant(
    val appId: String,
    val capability: Capability,
    val action: String,
    /** Null means an explicitly persistent app-level grant; non-null is session-bound. */
    val sessionId: String? = null,
    val enabled: Boolean = true
)

interface PermissionStore {
    fun isGranted(appId: String, capability: Capability, action: String, sessionId: String?): Boolean
}

interface MutablePermissionStore : PermissionStore {
    fun grant(grant: CapabilityGrant)
    fun revoke(grant: CapabilityGrant)
}

class InMemoryPermissionStore(grants: Set<CapabilityGrant> = emptySet()) : MutablePermissionStore {
    private val grants = grants.toMutableSet()

    @Synchronized
    override fun isGranted(appId: String, capability: Capability, action: String, sessionId: String?): Boolean =
        grants.any { grant ->
            grant.enabled &&
                grant.appId == appId &&
                grant.capability == capability &&
                grant.action == action &&
                grant.sessionId == sessionId
        }

    @Synchronized
    override fun grant(grant: CapabilityGrant) { grants.add(grant) }

    @Synchronized
    override fun revoke(grant: CapabilityGrant) { grants.remove(grant) }
}
