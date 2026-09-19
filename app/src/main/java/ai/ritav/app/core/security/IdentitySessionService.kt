package ai.ritav.app.core.security

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Trusted identity-session issuance boundary.
 *
 * A protected execution session may only be created after the platform
 * authentication gateway reports success. Callers cannot self-assert identity.
 */
internal class IdentitySessionService(
    private val sessionManager: IdentitySessionManager,
    private val authenticationGateway: DeviceAuthorizationGateway,
    private val clockEpochMillis: () -> Long = System::currentTimeMillis,
    private val sensitiveFirewall: SensitiveInformationFirewall = SensitiveInformationFirewall()
) {
    fun authenticate(
        reason: String,
        callback: (session: SecuritySession?) -> Unit
    ) {
        val delivered = AtomicBoolean(false)
        fun deliver(session: SecuritySession?) {
            if (delivered.compareAndSet(false, true)) callback(session)
        }

        val validRequest = runCatching {
            reason.isNotBlank() &&
                reason.length <= MAX_REASON_LENGTH &&
                sensitiveFirewall.inspect(reason).allowed &&
                authenticationGateway.isDeviceAuthenticationAvailable()
        }.getOrDefault(false)

        if (!validRequest) {
            deliver(null)
            return
        }

        runCatching {
            authenticationGateway.authenticate(reason) { success ->
                if (!success) {
                    deliver(null)
                    return@authenticate
                }

                val session = runCatching {
                    val now = clockEpochMillis()
                    if (now < 0L) null
                    else sessionManager.createSession(
                        identity = IdentityLevel.TRUSTED_SIGNAL,
                        nowEpochMillis = now
                    )
                }.getOrNull()
                deliver(session)
            }
        }.onFailure {
            deliver(null)
        }
    }

    private companion object {
        const val MAX_REASON_LENGTH = 512
    }
}
