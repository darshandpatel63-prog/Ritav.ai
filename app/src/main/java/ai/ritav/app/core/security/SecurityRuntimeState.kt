package ai.ritav.app.core.security

import android.content.Context
import ai.ritav.app.core.storage.SecureLocalStore

/** Runtime security composition root with shared emergency-stop, permissions, and audit state. */
class SecurityRuntimeState private constructor(
    private val emergencyStopController: EmergencyStopController,
    private val mutablePermissionStore: MutablePermissionStore,
    private val secureLocalStore: SecureLocalStore?,
    val policyEngine: PolicyEngine,
    val auditLog: AuditLog
) {
    private constructor(deps: RuntimeDeps) : this(
        deps.emergencyStopController,
        deps.permissionStore,
        deps.secureLocalStore,
        deps.policyEngine,
        deps.auditLog
    )

    private constructor(
        emergencyStopController: EmergencyStopController,
        permissionStore: MutablePermissionStore,
        secureLocalStore: SecureLocalStore?,
        auditLog: AuditLog
    ) : this(
        emergencyStopController = emergencyStopController,
        mutablePermissionStore = permissionStore,
        secureLocalStore = secureLocalStore,
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
        secureLocalStore = null,
        auditLog = InMemoryAuditLog()
    )

    /** Internal mutable capability-store access is kept inside the security composition. */
    internal fun mutablePermissionStore(): MutablePermissionStore = mutablePermissionStore

    /** Internal encrypted storage access for security-owned durable state only. */
    internal fun secureLocalStore(): SecureLocalStore =
        requireNotNull(secureLocalStore) {
            "Secure local store is unavailable in the lightweight test runtime"
        }

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

    internal fun resumeAfterUserConfirmation(confirmed: Boolean) {
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
        val secureLocalStore = SecureLocalStore(context)
        val emergencyStopController = EmergencyStopController()
        val permissionStore: MutablePermissionStore = SecurePermissionStore(secureLocalStore)
        val policyEngine = PolicyEngine(
            permissionStore = permissionStore,
            emergencyStop = emergencyStopController
        )
        val auditLog: AuditLog = SecureAuditLog(secureLocalStore)
    }
}
