package ai.ritav.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RitavPlatformTest {
    @Test
    fun platformFamiliesAndFormFactorsAreExplicit() {
        assertTrue(RitavPlatform.entries.contains(RitavPlatform.ANDROID))
        assertTrue(RitavPlatform.entries.contains(RitavPlatform.IOS))
        assertTrue(RitavPlatform.entries.contains(RitavPlatform.IPADOS))
        assertTrue(RitavPlatform.entries.contains(RitavPlatform.WINDOWS))
        assertTrue(RitavPlatform.entries.contains(RitavPlatform.MACOS))
        assertTrue(RitavPlatform.entries.contains(RitavPlatform.LINUX))
        assertTrue(RitavPlatform.entries.contains(RitavPlatform.CHROMEOS))
        assertEquals(5, RitavFormFactor.entries.size)
    }

    @Test
    fun unavailableCapabilityIsNotPermission() {
        val profile = DeviceProfile(
            platform = RitavPlatform.LINUX,
            osVersion = "test",
            formFactor = RitavFormFactor.DESKTOP,
            capabilities = PlatformCapabilities(
                secureStorage = true,
                deviceAuthentication = true,
                voiceInput = false,
                screenCapture = false,
                accessibilityAutomation = false,
                backgroundExecution = false,
                notifications = true,
                localModelRuntime = true,
                networkAccess = false
            )
        )

        assertFalse(profile.capabilities.voiceInput)
        assertFalse(profile.capabilities.networkAccess)
    }
}
