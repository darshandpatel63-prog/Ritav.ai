package ai.ritav.app.core.security

import android.content.Context
import ai.ritav.app.core.storage.SecureLocalStore

/** Runtime security composition root for Ritav Android. */
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
            emergencyStop = emergencyStopController
        )
    )

    /** Lightweight constructor retained for unit tests. */
    constructor(emergencyStopController: EmergencyStopController = EmergencyStopController()) : this(
        emergencyStopController = emergencyStopController,
        policyEngine = PolicyEngine(emergencyStop = emergencyStopController)
    )

    fun activateEmergencyStop() = emergencyStopController.activate()

    fun resumeAfterUserConfirmation(confirmed: Boolean) =
        emergencyStopController.resetAfterExplicitUserConfirmation(confirmed)

    fun isEmergencyStopActive(): Boolean = emergencyStopController.isActive()

    fun isSafeModeActive(): Boolean = emergencyStopController.isActive()
}
