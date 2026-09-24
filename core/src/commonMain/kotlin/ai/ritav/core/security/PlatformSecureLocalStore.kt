package ai.ritav.core.security

/**
 * Platform-neutral bounded secure local state contract.
 *
 * Implementations must use a platform-native protected store and must fail closed
 * on invalid bounds or unavailable secure storage. This interface grants no
 * execution or authorization authority.
 */
interface PlatformSecureLocalStore {
    fun putString(name: String, value: String)
    fun getString(name: String): String?
    fun remove(name: String)
}
