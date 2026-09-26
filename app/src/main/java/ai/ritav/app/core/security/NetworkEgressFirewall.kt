package ai.ritav.app.core.security

/** Classification of data that may be considered for an outbound request. */
internal enum class DataClassification {
    PUBLIC,
    USER_DATA,
    SENSITIVE,
    SECRET
}

internal data class NetworkEgressRequest(
    val destination: String,
    val reason: String,
    val dataClassification: DataClassification,
    val userExplicitlyAuthorized: Boolean = false
)

internal data class NetworkEgressDecision(
    val allowed: Boolean,
    val reason: String
)

/**
 * Security-internal policy evaluator; callers never receive network-I/O authority.
 *
 * Defense-in-depth egress policy. The Android app currently has no INTERNET
 * permission, but this deterministic boundary also protects future connected
 * mode from accidentally sending private data through a new dependency.
 *
 * This type is intentionally internal: evaluating egress policy is not itself
 * an authority to perform network I/O or grant authorization to a caller.
 */
internal class NetworkEgressFirewall {
    fun evaluate(request: NetworkEgressRequest): NetworkEgressDecision {
        if (request.destination.isBlank()) return deny("Network destination is required")
        if (request.reason.isBlank()) return deny("Network purpose is required")

        return when (request.dataClassification) {
            DataClassification.SECRET -> deny("Secrets must never leave the device")
            DataClassification.SENSITIVE ->
                if (request.userExplicitlyAuthorized) {
                    NetworkEgressDecision(true, "Sensitive egress explicitly authorized")
                } else {
                    deny("Sensitive data requires explicit user authorization")
                }
            DataClassification.USER_DATA ->
                if (request.userExplicitlyAuthorized) {
                    NetworkEgressDecision(true, "User data egress explicitly authorized")
                } else {
                    deny("User data requires explicit user authorization")
                }
            DataClassification.PUBLIC ->
                NetworkEgressDecision(true, "Public-data egress permitted by classification policy")
        }
    }

    private fun deny(reason: String) = NetworkEgressDecision(false, reason)
}
