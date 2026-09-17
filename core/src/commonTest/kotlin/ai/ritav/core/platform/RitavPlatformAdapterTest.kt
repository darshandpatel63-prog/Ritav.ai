package ai.ritav.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class RitavPlatformAdapterTest {
    @Test
    fun adapterExposesPlatformAndFormFactorWithoutGrantingPermission() {
        val adapter = object : RitavPlatformAdapter {
            override fun deviceProfile(): DeviceProfile = DeviceProfile(
                platform = RitavPlatform.WINDOWS,
                osVersion = "test",
                formFactor = RitavFormFactor.LAPTOP,
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
        }

        val profile = adapter.deviceProfile()
        assertEquals(RitavPlatform.WINDOWS, profile.platform)
        assertEquals(RitavFormFactor.LAPTOP, profile.formFactor)
    }
}
