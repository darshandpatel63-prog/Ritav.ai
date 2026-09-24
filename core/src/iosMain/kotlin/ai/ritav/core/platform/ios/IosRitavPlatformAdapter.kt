package ai.ritav.core.platform.ios

import ai.ritav.core.platform.DeviceProfile
import ai.ritav.core.platform.PlatformCapabilities
import ai.ritav.core.platform.RitavFormFactor
import ai.ritav.core.platform.RitavPlatform
import ai.ritav.core.platform.RitavPlatformAdapter
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiom.UIUserInterfaceIdiomPad
import platform.UIKit.UIUserInterfaceIdiom.UIUserInterfaceIdiomPhone

/**
 * Conservative native iOS/iPadOS platform adapter.
 *
 * This adapter reports facts that can be established directly from UIKit.
 * Security-sensitive capabilities remain unavailable until Ritav has a
 * concrete, reviewed native runtime implementation for them.
 *
 * This is intentionally not a product-support claim.
 */
class IosRitavPlatformAdapter : RitavPlatformAdapter {
    override fun deviceProfile(): DeviceProfile {
        val device = UIDevice.currentDevice
        val formFactor = when (device.userInterfaceIdiom) {
            UIUserInterfaceIdiomPad -> RitavFormFactor.TABLET
            UIUserInterfaceIdiomPhone -> RitavFormFactor.PHONE
            else -> RitavFormFactor.OTHER
        }

        val platform = when (formFactor) {
            RitavFormFactor.TABLET -> RitavPlatform.IPADOS
            RitavFormFactor.PHONE -> RitavPlatform.IOS
            else -> RitavPlatform.IOS
        }

        return DeviceProfile(
            platform = platform,
            osVersion = listOf(device.systemName, device.systemVersion)
                .filter { it.isNotBlank() }
                .joinToString(" "),
            formFactor = formFactor,
            capabilities = PlatformCapabilities(
                secureStorage = false,
                deviceAuthentication = false,
                voiceInput = false,
                screenCapture = false,
                accessibilityAutomation = false,
                backgroundExecution = false,
                notifications = false,
                localModelRuntime = false,
                networkAccess = false
            )
        )
    }
}
