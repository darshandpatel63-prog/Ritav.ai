package ai.ritav.core.platform.ios

import ai.ritav.core.platform.RitavFormFactor
import ai.ritav.core.platform.RitavPlatform
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosRitavPlatformAdapterTest {
    @Test
    fun adapterReportsRealAppleDeviceIdentityWithoutGrantingCapabilities() {
        val profile = IosRitavPlatformAdapter().deviceProfile()

        assertTrue(
            profile.platform == RitavPlatform.IOS ||
                profile.platform == RitavPlatform.IPADOS
        )
        assertTrue(profile.osVersion.isNotBlank())
        assertTrue(
            profile.formFactor == RitavFormFactor.PHONE ||
                profile.formFactor == RitavFormFactor.TABLET ||
                profile.formFactor == RitavFormFactor.OTHER
        )

        assertFalse(profile.capabilities.secureStorage)
        assertFalse(profile.capabilities.deviceAuthentication)
        assertFalse(profile.capabilities.voiceInput)
        assertFalse(profile.capabilities.screenCapture)
        assertFalse(profile.capabilities.accessibilityAutomation)
        assertFalse(profile.capabilities.backgroundExecution)
        assertFalse(profile.capabilities.notifications)
        assertFalse(profile.capabilities.localModelRuntime)
        assertFalse(profile.capabilities.networkAccess)
    }
}
