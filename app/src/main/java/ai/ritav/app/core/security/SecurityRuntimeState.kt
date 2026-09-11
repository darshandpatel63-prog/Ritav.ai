package ai.ritav.app.core.security

/**
 * Single process-local source of truth for the user-visible security mode.
 *
 * This state is deliberately small. Persistent security settings will later be
 * backed by encrypted storage; an emergency stop remains fail-closed in the
 * current process until explicitly resumed by the user.
 */
class SecurityRuntimeState(
    private val emergencyStopController: EmergencyStopController = EmergencyStopController()
) {
    fun activateEmergencyStop() {
        emergencyStopController.activate()
    }

    fun resumeAfterUserConfirmation(confirmed: Boolean) {
        emergencyStopController.resetAfterExplicitUserConfirmation(confirmed)
    }

    fun isEmergencyStopActive(): Boolean = emergencyStopController.isActive()

    fun isSafeModeActive(): Boolean = emergencyStopController.isActive()
}
