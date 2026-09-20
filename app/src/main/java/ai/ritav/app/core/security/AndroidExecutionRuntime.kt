package ai.ritav.app.core.security

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
    private val emergencyStop = securityState.policyEngine.emergencyStopController()

    private val authorizationGate = ActionAuthorizationGate(emergencyStop)
    private val identitySessionManager = IdentitySessionManager(emergencyStop)
    private val deviceAuthorization = AndroidDeviceAuthorizationGateway(activity)
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
            deviceAuthorization = deviceAuthorization,
            emergencyStop = emergencyStop
        )

    /**
     * Trusted identity-session issuance for protected actions.
     * Session creation is possible only after platform authentication succeeds.
     */
    internal val identitySessionService: IdentitySessionService =
        IdentitySessionService(
            sessionManager = identitySessionManager,
            authenticationGateway = deviceAuthorization,
            emergencyStop = emergencyStop
        )

    internal val capabilityGrantService: CapabilityGrantService =
        CapabilityGrantService(
            registry = capabilityRegistry,
            permissionStore = securityState.mutablePermissionStore(),
            authorizationGate = authorizationGate,
            emergencyStop = emergencyStop
        )
}
