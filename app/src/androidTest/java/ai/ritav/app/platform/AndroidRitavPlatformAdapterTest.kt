package ai.ritav.app.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidRitavPlatformAdapterTest {
    @Test
    fun detectsChromeOsByHostFeatureWithoutGrantingPermissions() {
        assertEquals(
            ai.ritav.core.platform.RitavPlatform.CHROMEOS,
            AndroidRitavPlatformAdapter.detectPlatform { feature ->
                feature == "org.chromium.arc"
            }
        )
    }

    @Test
    fun detectsAndroidWhenChromeOsFeatureIsAbsent() {
        assertEquals(
            ai.ritav.core.platform.RitavPlatform.ANDROID,
            AndroidRitavPlatformAdapter.detectPlatform { false }
        )
    }

    @Test
    fun reportsActualAndroidHostWithoutGrantingAppPermissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val profile = AndroidRitavPlatformAdapter(context).deviceProfile()

        val expectedPlatform =
            if (context.packageManager.hasSystemFeature("org.chromium.arc")) {
                ai.ritav.core.platform.RitavPlatform.CHROMEOS
            } else {
                ai.ritav.core.platform.RitavPlatform.ANDROID
            }
        assertEquals(expectedPlatform, profile.platform)
        assertNotNull(profile.osVersion)
        assertNotNull(profile.formFactor)
    }
}
