package ai.ritav.app.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.AndroidActionAdapter
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ExecutionResult

/**
 * Minimal production Android action adapter.
 *
 * It deliberately exposes only APP_LAUNCH + "open" and requires a deterministic
 * dispatch-state expectation. The common security boundary remains authoritative.
 */
class AndroidIntentActionAdapter internal constructor(
    dispatcher: AndroidAppLaunchDispatcher,
    private val isTrustedPackage: (String) -> Boolean
) : AndroidActionAdapter {

    constructor(
        context: Context,
        registry: ai.ritav.app.core.security.AppCapabilityRegistry
    ) : this(
        dispatcher = ContextAndroidAppLaunchDispatcher(context.applicationContext),
        isTrustedPackage = AndroidPackageIdentityVerifier(context.applicationContext, registry)::isTrusted
    )

    override fun execute(plan: ActionPlan): ExecutionResult {
        if (!plan.isValid()) {
            return ExecutionResult(false, false, "Action plan is malformed")
        }
        if (plan.capability != Capability.APP_LAUNCH || plan.action != OPEN_ACTION) {
            return ExecutionResult(false, false, "Android adapter does not support this action")
        }
        if (plan.expectedState != LAUNCH_DISPATCHED_STATE) {
            return ExecutionResult(false, false, "Launch action requires dispatch-state verification")
        }
        if (!isTrustedPackage(plan.appId)) {
            return ExecutionResult(false, false, "Target package identity is not trusted")
        }

        val dispatched = runCatching {
            dispatcher.dispatchLaunch(plan.appId)
        }.getOrDefault(false)

        return if (dispatched) {
            ExecutionResult(
                success = true,
                verified = false,
                message = "Android launch request dispatched; final UI state is not independently observed",
                observedState = LAUNCH_DISPATCHED_STATE
            )
        } else {
            ExecutionResult(
                success = false,
                verified = false,
                message = "Android launch request could not be dispatched"
            )
        }
    }

    private companion object {
        const val OPEN_ACTION = "open"
        const val LAUNCH_DISPATCHED_STATE = "LAUNCH_DISPATCHED"
    }
}


/**
 * Verifies the installed package identity against trusted registry metadata.
 * Package-name equality alone is insufficient because a package can be replaced
 * after an uninstall/reinstall event.
 */
private class AndroidPackageIdentityVerifier(
    private val context: Context,
    private val registry: ai.ritav.app.core.security.AppCapabilityRegistry
) {
    fun isTrusted(packageName: String): Boolean {
        val expected = registry.trustedCertificateSha256(packageName)?.lowercase() ?: return false
        return runCatching {
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
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            signatures.any { signature ->
                sha256(signature.toByteArray()) == expected
            }
        }.getOrDefault(false)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
