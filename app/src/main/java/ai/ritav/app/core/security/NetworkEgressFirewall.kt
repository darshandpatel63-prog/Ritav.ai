package ai.ritav.app.core.security

/** Classification of data that may be considered for an outbound request. */
enum class DataClassification {
    PUBLIC,
    USER_DATA,
    SENSITIVE,
    SECRET
}

data class NetworkEgressRequest(
    val destination: String,
    val reason: String,
    val dataClassification: DataClassification,
    val userExplicitlyAuthorized: Boolean = false
)

data class NetworkEgressDecision(
    val allowed: Boolean,
    val reason: String
)

/**
 * Defense-in-depth egress policy. The Android app currently has no INTERNET
 * permission, but this deterministic boundary also protects future connected
 * mode from accidentally sending private data through a new dependency.
 */
class NetworkEgressFirewall {
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
            DataClassification.PUBLIC,
            DataClassification.USER_DATA ->
                NetworkEgressDecision(true, "Egress permitted by current classification policy")
        }
    }

    private fun deny(reason: String) = NetworkEgressDecision(false, reason)
}
