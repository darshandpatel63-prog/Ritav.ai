package ai.ritav.app.core.security

import androidx.fragment.app.FragmentActivity
import ai.ritav.app.platform.AndroidIntentActionAdapter
import ai.ritav.app.platform.AndroidTrustedAppProvisioningCoordinator
import ai.ritav.app.platform.AndroidTrustedPackageEvidenceReader
import ai.ritav.app.core.orchestrator.AndroidModelContextConsumer

/**
 * Single Android security/execution composition root.
 *
 * Persisted trusted-app metadata is loaded only from the security-owned encrypted
 * store. Missing or invalid state yields an empty registry and therefore
 * deny-by-default external execution.
 */
class AndroidExecutionRuntime(
    activity: FragmentActivity
) {
    val securityState = SecurityRuntimeState(activity.applicationContext)
    private val emergencyStop = securityState.policyEngine.emergencyStopController()
    private val trustedAppEntryStore = SecureTrustedAppEntryStore(securityState.secureLocalStore())

    private val capabilityRegistry = AppCapabilityRegistry(
        when (val snapshot = trustedAppEntryStore.snapshot()) {
            TrustedAppEntrySnapshot.Unconfigured -> emptyList()
            TrustedAppEntrySnapshot.Invalid -> emptyList()
            is TrustedAppEntrySnapshot.Loaded -> snapshot.specs
        }
    )

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

    /** Single sanctioned ingress from Android app context into agent/model reasoning. */
    internal val modelContextConsumer: AndroidModelContextConsumer = AndroidModelContextConsumer()

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

    private val capabilityGrantService: CapabilityGrantService =
        CapabilityGrantService(
            registry = capabilityRegistry,
            permissionStore = securityState.mutablePermissionStore(),
            authorizationGate = authorizationGate,
            emergencyStop = emergencyStop,
            identitySessionManager = identitySessionManager
        )

    internal val capabilityGrantCoordinator: CapabilityGrantCoordinator =
        CapabilityGrantCoordinator(
            registry = capabilityRegistry,
            grantService = capabilityGrantService,
            authorizationService = authorizationService,
            identitySessionManager = identitySessionManager
        )

    private val trustedAppProvisioningService =
        TrustedAppProvisioningService(
            registry = capabilityRegistry,
            entryStore = trustedAppEntryStore,
            authorizationGate = authorizationGate,
            emergencyStop = emergencyStop,
            identitySessionManager = identitySessionManager
        )

    internal val trustedAppProvisioningCoordinator =
        AndroidTrustedAppProvisioningCoordinator(
            evidenceReader = AndroidTrustedPackageEvidenceReader(activity.applicationContext),
            provisioningService = trustedAppProvisioningService,
            authorizationService = authorizationService,
            identitySessionManager = identitySessionManager
        )

    /** Trusted identity-session issuance for protected actions. */
    internal val identitySessionService: IdentitySessionService =
        IdentitySessionService(
            sessionManager = identitySessionManager,
            authenticationGateway = deviceAuthorization,
            emergencyStop = emergencyStop
        )
}
