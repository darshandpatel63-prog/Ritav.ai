package ai.ritav.app.core.security

import android.content.Context
import ai.ritav.app.core.storage.SecureLocalStore

/**
 * Runtime security composition root.
 * A single EmergencyStopController is shared by the user-facing runtime state
 * and PolicyEngine so a stop is observed consistently by every layer.
 */
class SecurityRuntimeState private constructor(
    private val emergencyStopController: EmergencyStopController,
    val policyEngine: PolicyEngine
) {
    constructor(context: Context) : this(
        emergencyStopController = EmergencyStopController(),
        policyEngine = run {
            val controller = EmergencyStopController()
            PolicyEngine(
                permissionStore = SecurePermissionStore(
                    SecureLocalStore(context.applicationContext)
                ),
                emergencyStop = controller
            )
        }
    )

    /** Constructor retained for lightweight unit tests. */
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
