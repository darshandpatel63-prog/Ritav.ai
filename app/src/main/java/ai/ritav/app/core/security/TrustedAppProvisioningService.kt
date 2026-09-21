package ai.ritav.app.core.security

/**
 * Deterministic trust-entry mutation boundary.
 *
 * A trust entry changes future execution authority, so every write is bound to
 * an exact package, certificate digest, identity session, device-auth token and
 * Emergency-Stop generation. No caller can directly mutate the registry/store.
 */
internal class TrustedAppProvisioningService(
    private val registry: AppCapabilityRegistry,
    private val entryStore: TrustedAppEntryStore,
    private val authorizationGate: ActionAuthorizationGate,
    private val emergencyStop: EmergencyStopController = authorizationGate.emergencyStopController(),
    private val identitySessionManager: IdentitySessionManager = IdentitySessionManager(emergencyStop)
) {
    internal class Plan private constructor(
        val actionPlan: ActionPlan,
        private val certificateSha256: String
    ) {
        internal fun matchesEvidence(
            packageName: String,
            certificateSha256: String,
            signerCount: Int
        ): Boolean =
            packageName == actionPlan.appId &&
                signerCount == 1 &&
                normalizeCertificate(certificateSha256) == this.certificateSha256
    }

    fun createPlan(
        packageName: String,
        certificateSha256: String,
        signerCount: Int,
        identitySession: SecuritySession?,
        nowEpochMillis: Long
    ): Plan? {
        val normalizedCertificate = normalizeCertificate(certificateSha256) ?: return null
        if (signerCount != 1 ||
            identitySession == null ||
            !identitySessionManager.permitsProtectedCapability(identitySession, nowEpochMillis)
        ) return null

        val spec = specFor(packageName, normalizedCertificate) ?: return null
        if (registry.isRegistered(packageName)) return null

        val actionPlan = ActionPlan(
            appId = packageName,
            capability = Capability.APP_LAUNCH,
            action = TRUST_ADD_ACTION,
            riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE,
            expectedState = TRUST_ENTRY_ADD_STATE,
            sessionId = identitySession.id
        )
        if (!actionPlan.isValid()) return null

        return Plan(
            actionPlan = actionPlan,
            certificateSha256 = normalizedCertificate
        )
    }

    fun persist(
        plan: Plan,
        currentPackageName: String,
        currentCertificateSha256: String,
        currentSignerCount: Int,
        authorizationToken: String?,
        nowEpochMillis: Long,
        identitySession: SecuritySession?
    ): Boolean {
        return emergencyStop.runIfInactive {
            if (!isValidPlan(plan)) return@runIfInactive false
            if (currentPackageName != plan.actionPlan.appId) return@runIfInactive false

            val currentCertificate = normalizeCertificate(currentCertificateSha256)
                ?: return@runIfInactive false
            if (currentCertificate != plan.certificateSha256 || currentSignerCount != 1) {
                return@runIfInactive false
            }

            val session = identitySession ?: return@runIfInactive false
            if (plan.actionPlan.sessionId != session.id ||
                !identitySessionManager.permitsProtectedCapability(session, nowEpochMillis)
            ) return@runIfInactive false

            if (!authorizationGate.consume(
                    authorizationToken.orEmpty(),
                    plan.actionPlan,
                    AuthorizationLevel.DEVICE_AUTHENTICATION,
                    nowEpochMillis
                )
            ) return@runIfInactive false

            if (registry.isRegistered(plan.actionPlan.appId)) return@runIfInactive false

            val spec = specFor(plan.actionPlan.appId, plan.certificateSha256)
                ?: return@runIfInactive false

            if (!entryStore.add(spec)) return@runIfInactive false

            if (registry.addReviewedTrustedAppSpec(spec)) {
                true
            } else {
                entryStore.remove(spec)
                false
            }
        } ?: false
    }

    internal fun matchesEvidence(
        plan: Plan,
        packageName: String,
        certificateSha256: String,
        signerCount: Int
    ): Boolean =
        isValidPlan(plan) &&
            plan.matchesEvidence(packageName, certificateSha256, signerCount)

    private fun isValidPlan(plan: Plan): Boolean {
        if (!plan.actionPlan.isValid()) return false
        if (plan.actionPlan.capability != Capability.APP_LAUNCH ||
            plan.actionPlan.action != TRUST_ADD_ACTION ||
            plan.actionPlan.riskTier != RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE ||
            plan.actionPlan.expectedState != TRUST_ENTRY_ADD_STATE ||
            plan.actionPlan.sessionId == null
        ) return false

        val normalizedCertificate = normalizeCertificate(plan.certificateSha256) ?: return false
        return normalizedCertificate == plan.certificateSha256 &&
            specFor(plan.actionPlan.appId, normalizedCertificate) != null
    }

    private fun specFor(packageName: String, certificateSha256: String): AppCapabilitySpec? =
        AppCapabilitySpec(
            packageName = packageName,
            capability = Capability.APP_LAUNCH,
            actions = setOf(OPEN_ACTION),
            riskTier = RiskTier.TIER_1_REVERSIBLE,
            sensitiveContentBlocked = true,
            financialCategory = false,
            trustedCertificateSha256 = certificateSha256
        ).takeIf(::isValidReviewedTrustedAppSpec)

    private fun normalizeCertificate(value: String): String? =
        value.takeIf { it.matches(CERTIFICATE_DIGEST_REGEX) }?.lowercase()

    private companion object {
        const val OPEN_ACTION = "open"
        const val TRUST_ADD_ACTION = "trust:add:open"
        const val TRUST_ENTRY_ADD_STATE = "TRUST_ENTRY_ADD"
        val CERTIFICATE_DIGEST_REGEX = Regex("^[A-Fa-f0-9]{64}$")
    }
}
