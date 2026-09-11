package ai.ritav.app.core.security

class EmergencyStopController {
    @Volatile private var stopped: Boolean = false

    fun activate() { stopped = true }
    fun reset() { stopped = false }
    fun isActive(): Boolean = stopped
}
