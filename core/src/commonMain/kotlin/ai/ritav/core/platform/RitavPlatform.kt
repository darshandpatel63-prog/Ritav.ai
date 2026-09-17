package ai.ritav.core.platform

/** Supported platform families. Availability is determined by a real platform adapter. */
enum class RitavPlatform {
    ANDROID,
    IOS,
    IPADOS,
    WINDOWS,
    MACOS,
    LINUX,
    CHROMEOS
}

enum class RitavFormFactor {
    PHONE,
    TABLET,
    LAPTOP,
    DESKTOP,
    OTHER
}

/**
 * Runtime capability facts supplied by the concrete platform adapter.
 * False means unavailable; it must never be interpreted as permission.
 */
data class PlatformCapabilities(
    val secureStorage: Boolean,
    val deviceAuthentication: Boolean,
    val voiceInput: Boolean,
    val screenCapture: Boolean,
    val accessibilityAutomation: Boolean,
    val backgroundExecution: Boolean,
    val notifications: Boolean,
    val localModelRuntime: Boolean,
    val networkAccess: Boolean
)

data class DeviceProfile(
    val platform: RitavPlatform,
    val osVersion: String,
    val formFactor: RitavFormFactor,
    val capabilities: PlatformCapabilities
)
