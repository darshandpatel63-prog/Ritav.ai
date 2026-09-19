package ai.ritav.app.core.security

import android.content.Context
import ai.ritav.app.core.storage.SecureLocalStore

/** Runtime security composition root with shared emergency-stop, permissions, and audit state. */
class SecurityRuntimeState private constructor(
    private val emergencyStopController: EmergencyStopController,
    val permissionStore: PermissionStore,
    private val mutablePermissionStore: MutablePermissionStore,
    val policyEngine: PolicyEngine,
    val auditLog: AuditLog
) {
    private constructor(deps: RuntimeDeps) : this(
        deps.emergencyStopController,
        deps.permissionStore,
        deps.permissionStore,
        deps.policyEngine,
        deps.auditLog
    )

    private constructor(
        emergencyStopController: EmergencyStopController,
        permissionStore: MutablePermissionStore,
        auditLog: AuditLog
    ) : this(
        emergencyStopController = emergencyStopController,
        permissionStore = permissionStore,
        policyEngine = PolicyEngine(
            permissionStore = permissionStore,
            emergencyStop = emergencyStopController
        ),
        auditLog = auditLog
    )

    constructor(context: Context) : this(RuntimeDeps(context.applicationContext))

    /** Constructor retained for lightweight unit tests. */
    constructor(emergencyStopController: EmergencyStopController = EmergencyStopController()) : this(
        emergencyStopController = emergencyStopController,
        permissionStore = InMemoryPermissionStore(),
        auditLog = InMemoryAuditLog()
    )

    /** Internal mutable capability-store access is kept inside the security composition. */
    internal fun mutablePermissionStore(): MutablePermissionStore = mutablePermissionStore

    fun activateEmergencyStop() {
        emergencyStopController.activate()
        auditLog.append(
            AuditEvent(
                timestampEpochMillis = System.currentTimeMillis(),
                sessionId = null,
                actionHash = null,
                eventType = AuditEventType.EMERGENCY_STOP,
                allowed = true,
                verified = true,
                reason = "Emergency Stop activated"
            )
        )
    }

    fun resumeAfterUserConfirmation(confirmed: Boolean) {
        emergencyStopController.resetAfterExplicitUserConfirmation(confirmed)
        if (confirmed) {
            auditLog.append(
                AuditEvent(
                    timestampEpochMillis = System.currentTimeMillis(),
                    sessionId = null,
                    actionHash = null,
                    eventType = AuditEventType.EMERGENCY_STOP,
                    allowed = true,
                    verified = true,
                    reason = "Emergency Stop reset after explicit user confirmation"
                )
            )
        }
    }

    fun isEmergencyStopActive(): Boolean = emergencyStopController.isActive()

    fun isSafeModeActive(): Boolean = emergencyStopController.isActive()

    private class RuntimeDeps(context: Context) {
        private val secureStore = SecureLocalStore(context)
        val emergencyStopController = EmergencyStopController()
        val permissionStore: MutablePermissionStore = SecurePermissionStore(secureStore)
        val policyEngine = PolicyEngine(
            permissionStore = permissionStore,
            emergencyStop = emergencyStopController
        )
        val auditLog: AuditLog = SecureAuditLog(secureStore)
    }
}
