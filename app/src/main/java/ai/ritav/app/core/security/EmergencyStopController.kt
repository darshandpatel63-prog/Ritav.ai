package ai.ritav.app.core.security

/**
 * Process-local emergency stop for Ritav automation.
 *
 * Activation is immediate and intentionally requires no AI/model decision.
 * Resuming requires an explicit user action; future high-assurance flows may
 * additionally require device authentication before allowing reset.
 */
class EmergencyStopController {
    @Volatile
    private var stopped: Boolean = false

    fun activate() {
        stopped = true
    }

    /**
     * Resets only after an explicit user-controlled confirmation has occurred.
     * This is not a substitute for Android device authentication.
     */
    fun resetAfterExplicitUserConfirmation(confirmed: Boolean) {
        if (confirmed) stopped = false
    }

    fun isActive(): Boolean = stopped
}
