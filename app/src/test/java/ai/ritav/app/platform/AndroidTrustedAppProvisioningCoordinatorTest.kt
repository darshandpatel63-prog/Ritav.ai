package ai.ritav.app.platform

import ai.ritav.app.core.security.ActionAuthorizationGate
import ai.ritav.app.core.security.ActionAuthorizationService
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.AppCapabilitySpec
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.DeviceAuthorizationGateway
import ai.ritav.app.core.security.EmergencyStopController
import ai.ritav.app.core.security.IdentityLevel
import ai.ritav.app.core.security.IdentitySessionManager
import ai.ritav.app.core.security.StubDeviceAuthorizationGateway
import ai.ritav.app.core.security.TrustedAppEntrySnapshot
import ai.ritav.app.core.security.TrustedAppEntryStore
import ai.ritav.app.core.security.TrustedAppProvisioningService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTrustedAppProvisioningCoordinatorTest {
    private val certificateBytes = "ritav-test-certificate".toByteArray()

    private class FakeStore : TrustedAppEntryStore {
        val entries = mutableListOf<AppCapabilitySpec>()

        override fun snapshot(): TrustedAppEntrySnapshot =
            if (entries.isEmpty()) TrustedAppEntrySnapshot.Unconfigured
            else TrustedAppEntrySnapshot.Loaded(entries.toList())

        override fun add(spec: AppCapabilitySpec): Boolean {
            if (entries.any { it.packageName == spec.packageName }) return false
            entries += spec
            return true
        }

        override fun remove(spec: AppCapabilitySpec): Boolean = entries.remove(spec)
    }

    private class MutableCertificateReader(
        var certificates: List<ByteArray>? = listOf("initial".toByteArray())
    ) : AndroidPackageSigningCertificateReader {
        override fun read(packageName: String): List<ByteArray>? = certificates
    }

    private fun coordinator(
        reader: MutableCertificateReader,
        store: FakeStore,
        gate: ActionAuthorizationGate,
        sessionManager: IdentitySessionManager,
        deviceAuthorization: DeviceAuthorizationGateway
    ): AndroidTrustedAppProvisioningCoordinator {
        val stop = gate.emergencyStopController()
        val registry = AppCapabilityRegistry()
        val service = TrustedAppProvisioningService(registry, store, gate, stop, sessionManager)
        val auth = ActionAuthorizationService(
            gate = gate,
            deviceAuthorization = deviceAuthorization,
            clockEpochMillis = { 1_002L },
            emergencyStop = stop
        )
        return AndroidTrustedAppProvisioningCoordinator(
            evidenceReader = AndroidTrustedPackageEvidenceReader(reader),
            provisioningService = service,
            authorizationService = auth,
            identitySessionManager = sessionManager,
            clockEpochMillis = { 1_003L }
        )
    }

    @Test
    fun changedEvidenceBeforeAuthenticationBlocksApproval() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val reader = MutableCertificateReader(listOf(certificateBytes))
        val coordinator = coordinator(
            reader,
            store,
            gate,
            sessionManager,
            StubDeviceAuthorizationGateway(available = true, result = true)
        )

        val plan = requireNotNull(coordinator.prepare("com.example.safe", session))
        reader.certificates = listOf("changed".toByteArray())

        var result: Boolean? = null
        coordinator.approveAndPersist(plan, session, true) { result = it }

        assertFalse(result == true)
        assertTrue(store.entries.isEmpty())
    }

    @Test
    fun changedEvidenceAfterAuthenticationAlsoBlocksPersistence() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val reader = MutableCertificateReader(listOf(certificateBytes))

        val deviceAuthorization = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                reader.certificates = listOf("changed-after-auth".toByteArray())
                callback(true)
            }
        }
        val coordinator = coordinator(
            reader,
            store,
            gate,
            sessionManager,
            deviceAuthorization
        )

        val plan = requireNotNull(coordinator.prepare("com.example.safe", session))
        var result: Boolean? = null
        coordinator.approveAndPersist(plan, session, true) { result = it }

        assertFalse(result == true)
        assertTrue(store.entries.isEmpty())
    }

    @Test
    fun successfulDeviceAuthenticationPersistsEvidenceBackedTrust() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val reader = MutableCertificateReader(listOf(certificateBytes))
        val coordinator = coordinator(
            reader,
            store,
            gate,
            sessionManager,
            StubDeviceAuthorizationGateway(available = true, result = true)
        )

        val plan = requireNotNull(coordinator.prepare("com.example.safe", session))
        var result: Boolean? = null
        coordinator.approveAndPersist(plan, session, true) { result = it }

        assertTrue(result == true)
        assertTrue(store.entries.size == 1)
        assertTrue(store.entries.single().trustedCertificateSha256?.matches(Regex("^[a-f0-9]{64}$")) == true)
    }
    @Test
    fun successfulDeviceAuthenticationRemovesTrust() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val reader = MutableCertificateReader(listOf(certificateBytes))
        val coordinator = coordinator(
            reader,
            store,
            gate,
            sessionManager,
            StubDeviceAuthorizationGateway(available = true, result = true)
        )

        val addPlan = requireNotNull(coordinator.prepare("com.example.safe", session))
        var addResult: Boolean? = null
        coordinator.approveAndPersist(addPlan, session, true) { addResult = it }
        assertTrue(addResult == true)

        val removePlan = requireNotNull(coordinator.prepareRemoval("com.example.safe", session))
        var removeResult: Boolean? = null
        coordinator.approveAndRemove(removePlan, session, true) { removeResult = it }

        assertTrue(removeResult == true)
        assertTrue(store.entries.isEmpty())
    }

    @Test
    fun changedEvidenceBeforeRemovalAuthenticationBlocksApproval() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val reader = MutableCertificateReader(listOf(certificateBytes))
        val coordinator = coordinator(
            reader,
            store,
            gate,
            sessionManager,
            StubDeviceAuthorizationGateway(available = true, result = true)
        )

        val addPlan = requireNotNull(coordinator.prepare("com.example.safe", session))
        coordinator.approveAndPersist(addPlan, session, true) { }
        val removePlan = requireNotNull(coordinator.prepareRemoval("com.example.safe", session))
        reader.certificates = listOf("changed".toByteArray())

        var result: Boolean? = null
        coordinator.approveAndRemove(removePlan, session, true) { result = it }

        assertFalse(result == true)
        assertTrue(store.entries.size == 1)
    }
    @Test
    fun changedEvidenceAfterRemovalAuthenticationAlsoBlocksPersistence() {
        val stop = EmergencyStopController()
        val gate = ActionAuthorizationGate(stop)
        val sessionManager = IdentitySessionManager(stop)
        val session = sessionManager.createSession(IdentityLevel.TRUSTED_SIGNAL, 1_000L)
        val store = FakeStore()
        val reader = MutableCertificateReader(listOf(certificateBytes))
        var mutateAfterAuthentication = false

        val deviceAuthorization = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                if (mutateAfterAuthentication) {
                    reader.certificates = listOf("changed-after-auth".toByteArray())
                }
                callback(true)
            }
        }
        val coordinator = coordinator(
            reader, store, gate, sessionManager, deviceAuthorization
        )

        val addPlan = requireNotNull(coordinator.prepare("com.example.safe", session))
        coordinator.approveAndPersist(addPlan, session, true) { }
        reader.certificates = listOf(certificateBytes)

        val removePlan = requireNotNull(coordinator.prepareRemoval("com.example.safe", session))
        mutateAfterAuthentication = true

        var result: Boolean? = null
        coordinator.approveAndRemove(removePlan, session, true) { result = it }

        assertFalse(result == true)
        assertTrue(store.entries.size == 1)
    }}
