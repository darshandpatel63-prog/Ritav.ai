package ai.ritav.app.core.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import ai.ritav.app.platform.AndroidIntentActionAdapter

/**
 * Single Android security/execution composition root.
 *
 * The capability registry is trusted application configuration. An empty
 * registry is valid and keeps external app execution unavailable.
 */
class AndroidExecutionRuntime(
    activity: FragmentActivity,
    capabilityRegistry: AppCapabilityRegistry
) {
    val securityState = SecurityRuntimeState(activity.applicationContext)

    private val authorizationGate = ActionAuthorizationGate()
    private val identitySessionManager = IdentitySessionManager()
    private val securityPipeline = SecurityExecutionPipeline(
        policyEngine = securityState.policyEngine,
        executionPolicyGate = ExecutionPolicyGate(securityState.policyEngine),
        authorizationGate = authorizationGate,
        identitySessionManager = identitySessionManager,
        auditLog = securityState.auditLog
    )

    val executionBridge: ExecutionBridge = ExecutionBridge(
        capabilityPolicyGate = CapabilityPolicyGate(capabilityRegistry),
        securityPipeline = securityPipeline,
        adapter = AndroidIntentActionAdapter(activity.applicationContext, capabilityRegistry)
    )

    val authorizationService: ActionAuthorizationService =
        ActionAuthorizationService(
            gate = authorizationGate,
            deviceAuthorization = AndroidDeviceAuthorizationGateway(activity)
        )

    val capabilityGrantService: CapabilityGrantService =
        CapabilityGrantService(
            registry = capabilityRegistry,
            permissionStore = securityState.permissionStore,
            authorizationGate = authorizationGate,
            emergencyStop = securityState.policyEngine.emergencyStopController()
        )
}
