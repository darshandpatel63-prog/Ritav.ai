package ai.ritav.app.core.security

/**
 * Safe product/UI-facing security control surface.
 *
 * Concrete authorization services, permission stores, trusted-app evidence,
 * and execution composition remain behind this deterministic facade.
 */
interface SecurityControlPort {
    fun capabilityGrantOptions(): List<CapabilityGrantCandidate>

    fun authenticateProtectedActions(
        reason: String,
        callback: (SecuritySession?) -> Unit
    )

    fun prepareCapabilityGrant(
        candidate: CapabilityGrantCandidate,
        identitySession: SecuritySession
    ): ActionPlan?

    fun approveCapabilityGrant(
        plan: ActionPlan,
        identitySession: SecuritySession,
        userConfirmed: Boolean,
        onComplete: (Boolean) -> Unit
    )

    fun trustedPackageNames(): List<String>

    fun prepareTrustedApp(
        packageName: String,
        identitySession: SecuritySession
    ): ActionPlan?

    fun approveTrustedApp(
        plan: ActionPlan,
        identitySession: SecuritySession,
        userConfirmed: Boolean,
        onComplete: (Boolean) -> Unit
    )

    fun prepareTrustedRemoval(
        packageName: String,
        identitySession: SecuritySession
    ): ActionPlan?

    fun approveTrustedRemoval(
        plan: ActionPlan,
        identitySession: SecuritySession,
        userConfirmed: Boolean,
        onComplete: (Boolean) -> Unit
    )

    fun isEmergencyStopActive(): Boolean

    fun isSafeModeActive(): Boolean

    fun activateEmergencyStop()

    fun resumeAfterUserConfirmation()
}
