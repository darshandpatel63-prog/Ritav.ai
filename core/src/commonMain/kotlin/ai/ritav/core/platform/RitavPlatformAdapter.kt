package ai.ritav.core.platform

/**
 * Platform-neutral contract implemented by each supported host platform.
 * Implementations expose runtime facts only; capability availability is never
 * treated as permission and cannot weaken deterministic security policy.
 */
interface RitavPlatformAdapter {
    fun deviceProfile(): DeviceProfile
}
