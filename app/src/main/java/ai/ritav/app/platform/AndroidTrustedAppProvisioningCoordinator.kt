package ai.ritav.app.platform

import ai.ritav.app.core.security.ActionAuthorizationService
import ai.ritav.app.core.security.IdentitySessionManager
import ai.ritav.app.core.security.SecuritySession
import ai.ritav.app.core.security.TrustedAppProvisioningService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android-facing composition for trusted-app provisioning.
 *
 * It obtains fresh platform evidence and delegates the deterministic mutation
 * to the core provisioning service. It re-checks evidence immediately before
 * and immediately after device authentication to close package-update TOCTOU.
 */
internal class AndroidTrustedAppProvisioningCoordinator(
    private val evidenceReader: AndroidTrustedPackageEvidenceReader,
    private val provisioningService: TrustedAppProvisioningService,
    private val authorizationService: ActionAuthorizationService,
    private val identitySessionManager: IdentitySessionManager,
    private val clockEpochMillis: () -> Long = System::currentTimeMillis
) {
    fun prepare(
        packageName: String,
        identitySession: SecuritySession
    ): TrustedAppProvisioningService.Plan? {
        val now = safeNow() ?: return null
        if (!identitySessionManager.permitsProtectedCapability(identitySession, now)) return null

        val evidence = evidenceReader.read(packageName) ?: return null
        return provisioningService.createPlan(
            packageName = evidence.packageName,
            certificateSha256 = evidence.certificateSha256,
            signerCount = evidence.signerCount,
            identitySession = identitySession,
            nowEpochMillis = now
        )
    }

    fun approveAndPersist(
        plan: TrustedAppProvisioningService.Plan,
        identitySession: SecuritySession,
        userConfirmed: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val completed = AtomicBoolean(false)

        fun complete(result: Boolean) {
            if (completed.compareAndSet(false, true)) onComplete(result)
        }

        if (!userConfirmed ||
            plan.actionPlan.sessionId != identitySession.id ||
            !plan.actionPlan.isValid()
        ) {
            complete(false)
            return
        }

        val packageName = plan.actionPlan.appId
        if (!matches(plan, evidenceReader.read(packageName))) {
            complete(false)
            return
        }

        authorizationService.issueDeviceAuthenticationToken(
            plan = plan.actionPlan,
            reason = "Approve trusted application: $packageName"
        ) { token ->
            if (token == null) {
                complete(false)
                return@issueDeviceAuthenticationToken
            }

            val latestEvidence = evidenceReader.read(packageName)
            if (latestEvidence == null || !matches(plan, latestEvidence)) {
                complete(false)
                return@issueDeviceAuthenticationToken
            }

            val now = safeNow()
            if (now == null ||
                !identitySessionManager.permitsProtectedCapability(identitySession, now)
            ) {
                complete(false)
                return@issueDeviceAuthenticationToken
            }

            complete(
                provisioningService.persist(
                    plan = plan,
                    currentPackageName = latestEvidence.packageName,
                    currentCertificateSha256 = latestEvidence.certificateSha256,
                    currentSignerCount = latestEvidence.signerCount,
                    authorizationToken = token,
                    nowEpochMillis = now,
                    identitySession = identitySession
                )
            )
        }
    }

    private fun matches(
        plan: TrustedAppProvisioningService.Plan,
        evidence: AndroidTrustedPackageEvidence?
    ): Boolean =
        evidence != null &&
            provisioningService.matchesEvidence(
                plan = plan,
                packageName = evidence.packageName,
                certificateSha256 = evidence.certificateSha256,
                signerCount = evidence.signerCount
            )

    private fun safeNow(): Long? =
        runCatching { clockEpochMillis() }.getOrNull()?.takeIf { it >= 0L }
}
