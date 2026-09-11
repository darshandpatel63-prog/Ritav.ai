package ai.ritav.app.core.security

import android.content.Context
import ai.ritav.app.core.storage.SecureLocalStore

/**
 * Runtime security composition root.
 *
 * Production Android code should obtain permissions through the encrypted local
 * store rather than the in-memory fallback. Emergency stop remains process-local
 * and fail-closed until explicitly resumed by the user.
 */
class SecurityRuntimeState private constructor(
    private val emergencyStopController: EmergencyStopController,
    val policyEngine: PolicyEngine
) {
    constructor(context: Context) : this(
        emergencyStopController = EmergencyStopController(),
        policyEngine = PolicyEngine(
            permissionStore = SecurePermissionStore(
                SecureLocalStore(context.applicationContext)
            ),
            emergencyStop = EmergencyStopController()
        )
    )

    /** Constructor retained for lightweight unit tests. */
    constructor(emergencyStopController: EmergencyStopController = EmergencyStopController()) : this(
        emergencyStopController = emergencyStopController,
        policyEngine = PolicyEngine(emergencyStop = emergencyStopController)
    )

    fun activateEmergencyStop() {
        emergencyStopController.activate()
    }

    fun resumeAfterUserConfirmation(confirmed: Boolean) {
        emergencyStopController.resetAfterExplicitUserConfirmation(confirmed)
    }

    fun isEmergencyStopActive(): Boolean = emergencyStopController.isActive()

    fun isSafeModeActive(): Boolean = emergencyStopController.isActive()
}
