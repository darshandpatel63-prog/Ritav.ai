package ai.ritav.app.core.security

import ai.ritav.app.platform.AndroidTrustedAppProvisioningCoordinator

/**
 * Android composition of the public security-control facade.
 *
 * This implementation is internal so callers see only SecurityControlPort.
 */
internal class AndroidSecurityControlPort(
    private val securityState: SecurityRuntimeState,
    private val identitySessionService: IdentitySessionService,
    private val capabilityGrantCoordinator: CapabilityGrantCoordinator,
    private val trustedAppProvisioningCoordinator: AndroidTrustedAppProvisioningCoordinator
) : SecurityControlPort {

    override fun capabilityGrantOptions(): List<CapabilityGrantCandidate> =
        capabilityGrantCoordinator.options()

    override fun authenticateProtectedActions(
        reason: String,
        callback: (SecuritySession?) -> Unit
    ) {
        identitySessionService.authenticate(reason, callback)
    }

    override fun prepareCapabilityGrant(
        candidate: CapabilityGrantCandidate,
        identitySession: SecuritySession
    ): ActionPlan? =
        capabilityGrantCoordinator.prepare(candidate, identitySession)

    override fun approveCapabilityGrant(
        plan: ActionPlan,
        identitySession: SecuritySession,
        userConfirmed: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        capabilityGrantCoordinator.approveAndGrant(
            plan = plan,
            identitySession = identitySession,
            userConfirmed = userConfirmed,
            onComplete = onComplete
        )
    }

    override fun trustedPackageNames(): List<String> =
        trustedAppProvisioningCoordinator.trustedPackageNames()

    override fun prepareTrustedApp(
        packageName: String,
        identitySession: SecuritySession
    ): ActionPlan? =
        trustedAppProvisioningCoordinator.prepare(packageName, identitySession)

    override fun approveTrustedApp(
        plan: ActionPlan,
        identitySession: SecuritySession,
        userConfirmed: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        trustedAppProvisioningCoordinator.approveAndPersist(
            plan = plan,
            identitySession = identitySession,
            userConfirmed = userConfirmed,
            onComplete = onComplete
        )
    }

    override fun prepareTrustedRemoval(
        packageName: String,
        identitySession: SecuritySession
    ): ActionPlan? =
        trustedAppProvisioningCoordinator.prepareRemoval(packageName, identitySession)

    override fun approveTrustedRemoval(
        plan: ActionPlan,
        identitySession: SecuritySession,
        userConfirmed: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        trustedAppProvisioningCoordinator.approveAndRemove(
            plan = plan,
            identitySession = identitySession,
            userConfirmed = userConfirmed,
            onComplete = onComplete
        )
    }

    override fun isEmergencyStopActive(): Boolean =
        securityState.isEmergencyStopActive()

    override fun isSafeModeActive(): Boolean =
        securityState.isSafeModeActive()

    override fun activateEmergencyStop() {
        securityState.activateEmergencyStop()
    }

    override fun resumeAfterUserConfirmation() {
        securityState.resumeAfterUserConfirmation(confirmed = true)
    }
}
