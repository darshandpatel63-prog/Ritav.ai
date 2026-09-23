package ai.ritav.app.platform

import android.content.ComponentName
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RitavAccessibilityServiceManifestInstrumentationTest {
    @Test fun accessibilityServiceIsSystemBoundAndNotExternallyExported() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, RitavAccessibilityService::class.java),
            PackageManager.GET_META_DATA
        )

        assertNotNull(info)
        assertEquals("android.permission.BIND_ACCESSIBILITY_SERVICE", info.permission)
        assertTrue(!info.exported)
        assertNotNull(info.metaData)
    }
}
