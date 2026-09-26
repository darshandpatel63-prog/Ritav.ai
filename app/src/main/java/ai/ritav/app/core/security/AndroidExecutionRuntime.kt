package ai.ritav.app.core.security

import androidx.fragment.app.FragmentActivity
import ai.ritav.app.platform.AndroidIntentActionAdapter
import ai.ritav.app.platform.AndroidTrustedAppProvisioningCoordinator
import ai.ritav.app.platform.AndroidTrustedPackageEvidenceReader
import ai.ritav.app.platform.AndroidAccessibilityContextGate
import ai.ritav.app.platform.AndroidAccessibilityModelContextBridge
import ai.ritav.app.platform.AndroidAccessibilityRuntimeBinding
import ai.ritav.app.platform.AndroidAccessibilityRuntimeRegistry
import ai.ritav.app.core.orchestrator.ModelBackedSpecialistAgent
import ai.ritav.app.core.orchestrator.ModelRuntime
import ai.ritav.app.core.orchestrator.SecureModelRuntimeGateway
import ai.ritav.app.core.orchestrator.UnavailableModelRuntime

/**
 * Single Android security/execution composition root.
 *
 * Persisted trusted-app metadata is loaded only from the security-owned encrypted
 * store. Missing or invalid state yields an empty registry and therefore
 * deny-by-default external execution.
 */
class AndroidExecutionRuntime(
    activity: FragmentActivity,
    modelRuntime: ModelRuntime = UnavailableModelRuntime
) {
    internal val securityState = SecurityRuntimeState(activity.applicationContext)
    private val emergencyStop = securityState.policyEngine.emergencyStopController()
    private val trustedAppEntryStore = SecureTrustedAppEntryStore(securityState.secureLocalStore())

    private val capabilityRegistry = AppCapabilityRegistry(
        when (val snapshot = trustedAppEntryStore.snapshot()) {
            TrustedAppEntrySnapshot.Unconfigured -> emptyList()
            TrustedAppEntrySnapshot.Invalid -> emptyList()
            is TrustedAppEntrySnapshot.Loaded -> snapshot.specs
        }
    )

    private val identitySessionManager = IdentitySessionManager(emergencyStop)
    private val deviceAuthorization = AndroidDeviceAuthorizationGateway(activity)
    internal val authorizationService: ActionAuthorizationService =
        ActionAuthorizationService(
            deviceAuthorization = deviceAuthorization,
            emergencyStop = emergencyStop
        )
    private val securityPipeline = SecurityExecutionPipeline(
        policyEngine = securityState.policyEngine,
        executionPolicyGate = ExecutionPolicyGate(securityState.policyEngine),
        authorizationService = authorizationService,
        identitySessionManager = identitySessionManager,
        auditLog = securityState.auditLog
    )

    val executionBridge: SecureExecutionPort = ExecutionBridge(
        capabilityPolicyGate = CapabilityPolicyGate(capabilityRegistry),
        securityPipeline = securityPipeline,
        adapter = AndroidIntentActionAdapter(activity.applicationContext, capabilityRegistry)
    )

    private val capabilityGrantService: CapabilityGrantService =
        CapabilityGrantService(
            registry = capabilityRegistry,
            permissionStore = securityState.mutablePermissionStore(),
            authorizationService = authorizationService,
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
            authorizationService = authorizationService,
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

    val securityControl: SecurityControlPort =
        AndroidSecurityControlPort(
            securityState = securityState,
            identitySessionService = identitySessionService,
            capabilityGrantCoordinator = capabilityGrantCoordinator,
            trustedAppProvisioningCoordinator = trustedAppProvisioningCoordinator
        )
    
    private val secureModelRuntimeGateway = SecureModelRuntimeGateway(modelRuntime)
    private val accessibilityModelAgent =
        ModelBackedSpecialistAgent("android-model", secureModelRuntimeGateway)

    internal val accessibilityContextGate =
        AndroidAccessibilityContextGate()

    internal val accessibilityContextBridge =
        AndroidAccessibilityModelContextBridge(
            modelAgent = accessibilityModelAgent,
            gate = accessibilityContextGate
        )

    init {
        AndroidAccessibilityRuntimeRegistry.install(
            AndroidAccessibilityRuntimeBinding(
                gate = accessibilityContextGate,
                bridge = accessibilityContextBridge,
                currentStopGeneration = { emergencyStop.generation() }
            )
        )
    }

    internal fun armAccessibilityModelContext(
        request: ai.ritav.app.core.orchestrator.AgentRequest,
        packageName: String,
        durationMillis: Long = DEFAULT_ACCESSIBILITY_SESSION_MILLIS
    ): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now < 0L || durationMillis <= 0L || durationMillis > DEFAULT_ACCESSIBILITY_SESSION_MILLIS) {
            return false
        }
        val expiresAt = now + durationMillis
        if (expiresAt < now) return false
        return accessibilityContextGate.arm(
            request = request,
            packageName = packageName,
            agentId = accessibilityModelAgent.id,
            expiresAtElapsedRealtime = expiresAt,
            nowElapsedRealtime = now,
            stopGeneration = emergencyStop.generation()
        )
    }

    internal fun disarmAccessibilityModelContext() {
        accessibilityContextGate.disarm()
    }

    fun close() {
        closeAccessibilityRuntime()
    }

    internal fun closeAccessibilityRuntime() {
        accessibilityContextGate.disarm()
        AndroidAccessibilityRuntimeRegistry.clear(accessibilityContextGate)
    }

    private companion object {
        const val DEFAULT_ACCESSIBILITY_SESSION_MILLIS = 30_000L
    }

}
