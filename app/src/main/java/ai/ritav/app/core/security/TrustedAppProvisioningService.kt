package ai.ritav.app.core.security

import java.util.concurrent.ConcurrentHashMap

/**
 * Deterministic trust-entry mutation boundary.
 *
 * A trust entry changes future execution authority, so every write is bound to
 * an exact package, certificate evidence, identity session, device-auth token,
 * Emergency-Stop generation and a short-lived pending plan. Certificate evidence
 * is retained only inside this service; callers receive only the ActionPlan.
 */
internal class TrustedAppProvisioningService(
    private val registry: AppCapabilityRegistry,
    private val entryStore: TrustedAppEntryStore,
    private val authorizationGate: ActionAuthorizationGate,
    private val emergencyStop: EmergencyStopController = authorizationGate.emergencyStopController(),
    private val identitySessionManager: IdentitySessionManager = IdentitySessionManager(emergencyStop),
    private val clockEpochMillis: () -> Long = System::currentTimeMillis
) {
    private data class PendingPlan(
        val certificateSha256: String,
        val sessionId: String,
        val emergencyStopGeneration: Long,
        val expiresAtEpochMillis: Long
    )

    private val pendingPlans = ConcurrentHashMap<String, PendingPlan>()

    fun createPlan(
        packageName: String,
        certificateSha256: String,
        signerCount: Int,
        identitySession: SecuritySession?,
        nowEpochMillis: Long
    ): ActionPlan? {
        val normalizedCertificate = normalizeCertificate(certificateSha256) ?: return null
        if (signerCount != 1 ||
            identitySession == null ||
            !identitySessionManager.permitsProtectedCapability(identitySession, nowEpochMillis)
        ) return null

        val spec = specFor(packageName, normalizedCertificate) ?: return null
        if (registry.isRegistered(packageName)) return null

        val snapshot = entryStore.snapshot()
        if (snapshot is TrustedAppEntrySnapshot.Invalid) return null
        if (snapshot is TrustedAppEntrySnapshot.Loaded &&
            snapshot.specs.any { it.packageName == packageName }
        ) return null

        return emergencyStop.runIfInactive {
            if (nowEpochMillis < 0L || nowEpochMillis > Long.MAX_VALUE - PENDING_TTL_MILLIS) {
                return@runIfInactive null
            }

            val plan = ActionPlan(
                appId = packageName,
                capability = Capability.APP_LAUNCH,
                action = TRUST_ADD_ACTION,
                riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE,
                expectedState = TRUST_ENTRY_ADD_STATE,
                sessionId = identitySession.id
            )
            if (!plan.isValid()) return@runIfInactive null

            purgeExpired(nowEpochMillis)
            val hash = plan.stableHash()
            if (pendingPlans.size >= MAX_PENDING_PLANS && !pendingPlans.containsKey(hash)) {
                return@runIfInactive null
            }
            pendingPlans[hash] = PendingPlan(
                certificateSha256 = normalizedCertificate,
                sessionId = identitySession.id,
                emergencyStopGeneration = emergencyStop.generation(),
                expiresAtEpochMillis = nowEpochMillis + PENDING_TTL_MILLIS
            )
            plan
        }
    }

    internal fun matchesEvidence(
        plan: ActionPlan,
        packageName: String,
        certificateSha256: String,
        signerCount: Int
    ): Boolean = emergencyStop.runIfInactive {
        val pending = pendingPlans[plan.stableHash()] ?: return@runIfInactive false
        isValidPlan(plan, pending) &&
            packageName == plan.appId &&
            signerCount == 1 &&
            normalizeCertificate(certificateSha256) == pending.certificateSha256
    } ?: false

    fun persist(
        plan: ActionPlan,
        currentPackageName: String,
        currentCertificateSha256: String,
        currentSignerCount: Int,
        authorizationToken: String?,
        nowEpochMillis: Long,
        identitySession: SecuritySession?
    ): Boolean {
        return emergencyStop.runIfInactive {
            val pending = pendingPlans[plan.stableHash()] ?: return@runIfInactive false
            if (!isValidPlan(plan, pending)) return@runIfInactive false
            if (pending.expiresAtEpochMillis < nowEpochMillis) {
                pendingPlans.remove(plan.stableHash(), pending)
                return@runIfInactive false
            }
            if (currentPackageName != plan.appId) return@runIfInactive false

            val currentCertificate = normalizeCertificate(currentCertificateSha256)
                ?: return@runIfInactive false
            if (currentCertificate != pending.certificateSha256 || currentSignerCount != 1) {
                return@runIfInactive false
            }

            val session = identitySession ?: return@runIfInactive false
            if (plan.sessionId != session.id ||
                pending.sessionId != session.id ||
                !identitySessionManager.permitsProtectedCapability(session, nowEpochMillis)
            ) return@runIfInactive false

            if (!authorizationGate.consume(
                    authorizationToken.orEmpty(),
                    plan,
                    AuthorizationLevel.DEVICE_AUTHENTICATION,
                    nowEpochMillis
                )
            ) return@runIfInactive false

            if (registry.isRegistered(plan.appId)) return@runIfInactive false

            val spec = specFor(plan.appId, pending.certificateSha256)
                ?: return@runIfInactive false

            if (!entryStore.add(spec)) return@runIfInactive false

            if (registry.addReviewedTrustedAppSpec(spec)) {
                pendingPlans.remove(plan.stableHash(), pending)
                true
            } else {
                entryStore.remove(spec)
                false
            }
        } ?: false
    }

    private fun isValidPlan(plan: ActionPlan, pending: PendingPlan): Boolean =
        plan.isValid() &&
            plan.capability == Capability.APP_LAUNCH &&
            plan.action == TRUST_ADD_ACTION &&
            plan.riskTier == RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE &&
            plan.expectedState == TRUST_ENTRY_ADD_STATE &&
            plan.sessionId != null &&
            pending.sessionId == plan.sessionId &&
            pending.emergencyStopGeneration == emergencyStop.generation() &&
            pending.expiresAtEpochMillis >= 0L &&
            normalizeCertificate(pending.certificateSha256) == pending.certificateSha256 &&
            specFor(plan.appId, pending.certificateSha256) != null

    private fun purgeExpired(nowEpochMillis: Long) {
        pendingPlans.entries.removeIf { nowEpochMillis > it.value.expiresAtEpochMillis }
    }

    private fun specFor(packageName: String, certificateSha256: String): AppCapabilitySpec =
        AppCapabilitySpec(
            packageName = packageName,
            capability = Capability.APP_LAUNCH,
            actions = setOf(OPEN_ACTION),
            riskTier = RiskTier.TIER_1_REVERSIBLE,
            sensitiveContentBlocked = true,
            financialCategory = false,
            trustedCertificateSha256 = certificateSha256
        ).also { require(isValidReviewedTrustedAppSpec(it)) }

    private fun normalizeCertificate(value: String): String? =
        value.takeIf { it.matches(CERTIFICATE_DIGEST_REGEX) }?.lowercase()

    private companion object {
        const val OPEN_ACTION = "open"
        const val TRUST_ADD_ACTION = "trust:add:open"
        const val TRUST_ENTRY_ADD_STATE = "TRUST_ENTRY_ADD"
        const val MAX_PENDING_PLANS = 16
        const val PENDING_TTL_MILLIS = 5 * 60_000L
        val CERTIFICATE_DIGEST_REGEX = Regex("^[A-Fa-f0-9]{64}$")
    }
}
