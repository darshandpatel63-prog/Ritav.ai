package ai.ritav.app.core.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production Android implementation of device authorization.
 * The system owns biometric/device-credential verification; Ritav only receives
 * the success/failure result and never sees the credential or biometric data.
 */
internal class AndroidDeviceAuthorizationGateway(
    private val activity: FragmentActivity
) : DeviceAuthorizationGateway {
    override fun isDeviceAuthenticationAvailable(): Boolean {
        val manager = BiometricManager.from(activity)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
        if (!isDeviceAuthenticationAvailable() || reason.isBlank()) {
            callback(false)
            return
        }

        val delivered = AtomicBoolean(false)
        fun deliver(success: Boolean) {
            if (delivered.compareAndSet(false, true)) callback(success)
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                    deliver(true)

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    deliver(false)

                override fun onAuthenticationFailed() {
                    // Keep the prompt alive for another biometric attempt.
                }
            }
        )

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authorize Ritav action")
            .setDescription(reason)
            .setConfirmationRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setDeviceCredentialAllowed(true)
        }

        runCatching { prompt.authenticate(builder.build()) }
            .onFailure { deliver(false) }
    }
}
