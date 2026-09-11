package ai.ritav.app.core.security

import android.content.Context
import ai.ritav.app.core.storage.SecureLocalStore

/** Runtime security composition root with one shared emergency-stop controller. */
class SecurityRuntimeState private constructor(
    private val emergencyStopController: EmergencyStopController,
    val policyEngine: PolicyEngine
) {
    private constructor(deps: RuntimeDeps) : this(deps.emergencyStopController, deps.policyEngine)

    constructor(context: Context) : this(RuntimeDeps(context.applicationContext))

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

    private class RuntimeDeps(context: Context) {
        val emergencyStopController = EmergencyStopController()
        val policyEngine = PolicyEngine(
            permissionStore = SecurePermissionStore(SecureLocalStore(context)),
            emergencyStop = emergencyStopController
        )
    }
}
