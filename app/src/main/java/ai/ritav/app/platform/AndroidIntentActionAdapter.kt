package ai.ritav.app.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.AndroidActionAdapter
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ExecutionResult
import ai.ritav.app.core.security.SemanticVerificationContract
import ai.ritav.app.core.security.VerificationEvidence
import ai.ritav.app.core.security.ExpectedActionStateRegistry

/**
 * Minimal production Android action adapter.
 *
 * It deliberately exposes only APP_LAUNCH + "open" and requires independent
 * target-app foreground observation after launch dispatch. The common security
 * boundary remains authoritative.
 */
internal class AndroidIntentActionAdapter internal constructor(
    private val dispatcher: AndroidAppLaunchDispatcher,
    private val isTrustedPackage: (String) -> Boolean,
    private val targetAppResultObserver: AndroidTargetAppResultObserver,
    private val clock: () -> Long = System::currentTimeMillis
) : AndroidActionAdapter {

    internal constructor(
        context: Context,
        registry: AppCapabilityRegistry
    ) : this(
        dispatcher = ContextAndroidAppLaunchDispatcher(context.applicationContext),
        isTrustedPackage = AndroidPackageIdentityVerifier(
            registry = registry,
            certificateReader = ContextAndroidPackageSigningCertificateReader(context.applicationContext)
        )::isTrusted,
        targetAppResultObserver = AndroidTargetAppForegroundObserver(context.applicationContext)
    )

    override fun execute(plan: ActionPlan): ExecutionResult {
        if (!plan.isValid()) {
            return ExecutionResult(false, false, "Action plan is malformed")
        }
        if (plan.capability != Capability.APP_LAUNCH || plan.action != ExpectedActionStateRegistry.OPEN_ACTION) {
            return ExecutionResult(false, false, "Android adapter does not support this action")
        }
        if (plan.expectedState != ExpectedActionStateRegistry.LAUNCH_DISPATCHED_STATE) {
            return ExecutionResult(false, false, "Launch action requires dispatch-state verification")
        }
        if (!isTrustedPackage(plan.appId)) {
            return ExecutionResult(false, false, "Target package identity is not trusted")
        }
        if (!runCatching { targetAppResultObserver.canObserve(plan.appId) }.getOrDefault(false)) {
            return ExecutionResult(false, false, "Independent target-app observation is unavailable")
        }

        val clockAvailable = runCatching { clock() }
            .getOrNull()
            ?.takeIf { it >= 0L } != null
        if (!clockAvailable) {
            return ExecutionResult(false, false, "Security clock unavailable")
        }

        val dispatched = runCatching {
            dispatcher.dispatchLaunch(plan.appId)
        }.getOrDefault(false)

        if (!dispatched) {
            return ExecutionResult(
                success = false,
                verified = false,
                message = "Android launch request could not be dispatched"
            )
        }

        val dispatchCompletedAtMillis = runCatching { clock() }
            .getOrNull()
            ?.takeIf { it >= 0L }

        val observation = dispatchCompletedAtMillis?.let {
            runCatching {
                targetAppResultObserver.observeForegroundAfterDispatch(
                    packageName = plan.appId,
                    dispatchStartedAtMillis = it
                )
            }.getOrNull()
        }

        val verificationEvidence = observation?.let {
            VerificationEvidence(
                evidenceType = SemanticVerificationContract.TARGET_APP_FOREGROUND,
                subject = it.packageName,
                observedAtMillis = it.observedAtMillis
            )
        }

        return ExecutionResult(
            success = true,
            // Kept for compatibility/diagnostics only. The central bridge does
            // not trust this flag; it verifies structured evidence independently.
            verified = verificationEvidence != null,
            message = if (verificationEvidence != null) {
                "Android launch dispatched and target package independently observed in the foreground"
            } else {
                "Android launch dispatched; target-app foreground observation failed"
            },
            observedState = ExpectedActionStateRegistry.LAUNCH_DISPATCHED_STATE,
            verificationEvidence = verificationEvidence
        )
    }

}

/**
 * Testable boundary between Android package metadata access and trust evaluation.
 * A missing/unreadable signer result is represented by null and fails closed.
 */
internal fun interface AndroidPackageSigningCertificateReader {
    fun read(packageName: String): List<ByteArray>?
}

/**
 * Android framework-backed certificate reader.
 */
internal class ContextAndroidPackageSigningCertificateReader(
    private val context: Context
) : AndroidPackageSigningCertificateReader {
    override fun read(packageName: String): List<ByteArray>? = runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return@runCatching null
            signingInfo.apkContentsSigners?.map { it.toByteArray() }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.map { it.toByteArray() }
        }
    }.getOrNull()
}

/**
 * Verifies an installed package identity against trusted registry metadata.
 * Package-name equality alone is insufficient because the installed package may
 * be replaced after uninstall/reinstall. Missing, unreadable, or multi-signer
 * identities fail closed.
 */
internal class AndroidPackageIdentityVerifier(
    private val registry: AppCapabilityRegistry,
    private val certificateReader: AndroidPackageSigningCertificateReader
) {
    fun isTrusted(packageName: String): Boolean {
        val expected = registry.trustedCertificateSha256(packageName)?.lowercase() ?: return false
        if (!registry.isRegistered(packageName)) return false

        val certificates = runCatching { certificateReader.read(packageName) }.getOrNull() ?: return false
        if (certificates.size != 1) return false

        return sha256(certificates.single()) == expected
    }

    private fun sha256(bytes: ByteArray): String =
        bytes.toHexSha256()

    private fun ByteArray.toHexSha256(): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
}
