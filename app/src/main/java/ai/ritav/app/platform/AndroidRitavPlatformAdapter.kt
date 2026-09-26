package ai.ritav.app.platform

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import ai.ritav.core.platform.DeviceProfile
import ai.ritav.core.platform.PlatformCapabilities
import ai.ritav.core.platform.RitavFormFactor
import ai.ritav.core.platform.RitavPlatform
import ai.ritav.core.platform.RitavPlatformAdapter
import java.security.KeyStore

/**
 * Android runtime capability adapter. It reports host facts only; permission
 * and authorization decisions remain in the existing security pipeline.
 */
class AndroidRitavPlatformAdapter(
    private val context: Context
) : RitavPlatformAdapter {

    override fun deviceProfile(): DeviceProfile {
        return DeviceProfile(
            platform = detectPlatform(),
            osVersion = Build.VERSION.RELEASE.orEmpty(),
            formFactor = detectFormFactor(),
            capabilities = PlatformCapabilities(
                secureStorage = hasAndroidKeyStore(),
                deviceAuthentication = isDeviceSecure(),
                voiceInput = hasFeature(PackageManager.FEATURE_MICROPHONE),
                screenCapture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP,
                accessibilityAutomation = isAccessibilityServiceEnabled(),
                backgroundExecution = false,
                notifications = notificationsAvailable(),
                localModelRuntime = false,
                networkAccess = hasValidatedInternet()
            )
        )
    }

    private fun hasFeature(feature: String): Boolean =
        context.packageManager.hasSystemFeature(feature)

    private fun hasAndroidKeyStore(): Boolean = runCatching {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        true
    }.getOrDefault(false)

    private fun isDeviceSecure(): Boolean {
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        return keyguard?.isDeviceSecure == true
    }

    private fun notificationsAvailable(): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager?.areNotificationsEnabled() == true
    }

    private fun hasValidatedInternet(): Boolean = runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val packagePrefix = context.packageName + "/"
        return enabled.split(':').any { it.startsWith(packagePrefix) }
    }

    private fun detectPlatform(): RitavPlatform =
        detectPlatform { feature -> hasFeature(feature) }

    internal fun detectPlatform(hasSystemFeature: (String) -> Boolean): RitavPlatform =
        if (hasSystemFeature(CHROMEOS_SYSTEM_FEATURE)) {
            RitavPlatform.CHROMEOS
        } else {
            RitavPlatform.ANDROID
        }

    private fun detectFormFactor(): RitavFormFactor {
        val smallestWidthDp = context.resources.configuration.smallestScreenWidthDp
        return when {
            smallestWidthDp >= 600 -> RitavFormFactor.TABLET
            else -> RitavFormFactor.PHONE
        }
    }
}
