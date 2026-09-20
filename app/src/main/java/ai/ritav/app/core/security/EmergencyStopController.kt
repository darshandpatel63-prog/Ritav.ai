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

    @Synchronized
    fun activate() {
        stopped = true
    }

    /**
     * Resets only after an explicit user-controlled confirmation has occurred.
     * This is not a substitute for Android device authentication.
     */
    @Synchronized
    internal fun resetAfterExplicitUserConfirmation(confirmed: Boolean) {
        if (confirmed) stopped = false
    }

    /**
     * Atomically checks the stop state and runs a security-sensitive operation.
     * Activation/reset use the same monitor, so the operation cannot start
     * concurrently with an Emergency Stop transition.
     */
    internal fun <T> runIfInactive(operation: () -> T): T? = synchronized(this) {
        if (stopped) null else operation()
    }

    fun isActive(): Boolean = stopped
}
