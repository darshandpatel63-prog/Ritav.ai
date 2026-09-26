package ai.ritav.app.core.security

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Trusted caller composition for user-facing capability grants.
 *
 * UI code receives only sanitized registry candidates and an exact ActionPlan.
 * It never receives authorization enums, token-minting access, permission-store
 * mutation access, or certificate material.
 */
internal class CapabilityGrantCoordinator(
    private val registry: AppCapabilityRegistry,
    private val grantService: CapabilityGrantService,
    private val authorizationService: ActionAuthorizationService,
    private val identitySessionManager: IdentitySessionManager,
    private val clockEpochMillis: () -> Long = System::currentTimeMillis
) {
    fun options(): List<CapabilityGrantCandidate> =
        registry.capabilityGrantCandidates()

    /**
     * Creates a least-privilege, session-bound grant plan only for an active
     * trusted identity session.
     */
    fun prepare(
        candidate: CapabilityGrantCandidate,
        identitySession: SecuritySession
    ): ActionPlan? {
        val now = safeNow() ?: return null
        if (!options().contains(candidate)) return null
        if (!identitySessionManager.permitsProtectedCapability(identitySession, now)) return null

        return grantService.createGrantPlan(
            packageName = candidate.packageName,
            capability = candidate.capability,
            action = candidate.action,
            sessionId = identitySession.id
        )
    }

    /**
     * Applies the authorization flow selected by the deterministic grant plan:
     * Tier 2 requires an explicit user confirmation; Tier 3 requires platform
     * device authentication. The final permission mutation remains inside the
     * CapabilityGrantService security boundary.
     */
    fun approveAndGrant(
        plan: ActionPlan,
        identitySession: SecuritySession,
        userConfirmed: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val completed = AtomicBoolean(false)

        fun complete(result: Boolean) {
            if (completed.compareAndSet(false, true)) onComplete(result)
        }

        if (!userConfirmed ||
            !grantService.isGrantPlan(plan) ||
            plan.sessionId == null ||
            identitySession.id != plan.sessionId
        ) {
            complete(false)
            return
        }

        when (plan.riskTier) {
            RiskTier.TIER_2_CONTENT_MUTATION -> {
                val issueTime = safeNow()
                if (issueTime == null ||
                    !identitySessionManager.permitsProtectedCapability(identitySession, issueTime)
                ) {
                    complete(false)
                    return
                }

                val token = authorizationService.issueUserConfirmationToken(
                    plan = plan,
                    confirmedPlanHash = plan.stableHash()
                )
                if (token == null) {
                    complete(false)
                    return
                }

                val grantTime = safeNow()
                if (grantTime == null ||
                    !identitySessionManager.permitsProtectedCapability(identitySession, grantTime)
                ) {
                    complete(false)
                    return
                }

                complete(
                    grantService.grant(
                        plan = plan,
                        authorizationToken = token,
                        nowEpochMillis = grantTime,
                        identitySession = identitySession
                    )
                )
            }

            RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE -> {
                val reason =
                    "Approve capability access for " +
                        plan.appId +
                        ":" +
                        plan.action.removePrefix("grant:")

                authorizationService.issueDeviceAuthenticationToken(plan, reason) { token ->
                    if (token == null) {
                        complete(false)
                        return@issueDeviceAuthenticationToken
                    }

                    val grantTime = safeNow()
                    if (grantTime == null ||
                        !identitySessionManager.permitsProtectedCapability(identitySession, grantTime)
                    ) {
                        complete(false)
                        return@issueDeviceAuthenticationToken
                    }

                    complete(
                        grantService.grant(
                            plan = plan,
                            authorizationToken = token,
                            nowEpochMillis = grantTime,
                            identitySession = identitySession
                        )
                    )
                }
            }

            else -> complete(false)
        }
    }

    private fun safeNow(): Long? =
        runCatching { clockEpochMillis() }
            .getOrNull()
            ?.takeIf { it >= 0L }
}
