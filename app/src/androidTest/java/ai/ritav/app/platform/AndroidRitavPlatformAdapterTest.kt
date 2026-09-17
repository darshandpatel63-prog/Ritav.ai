package ai.ritav.app.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidRitavPlatformAdapterTest {
    @Test
    fun reportsActualAndroidHostWithoutGrantingAppPermissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val profile = AndroidRitavPlatformAdapter(context).deviceProfile()

        assertEquals(ai.ritav.core.platform.RitavPlatform.ANDROID, profile.platform)
        assertNotNull(profile.osVersion)
        assertNotNull(profile.formFactor)
    }
}
