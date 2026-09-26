package ai.ritav.app.platform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidTargetAppForegroundObserverInstrumentationTest {
    @Test
    fun usageStatsReaderIsAccessibleOnManagedDevice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        instrumentation.uiAutomation
            .executeShellCommand(
                "appops set " + context.packageName + " android:get_usage_stats allow"
            )
            .close()

        val observer = AndroidTargetAppForegroundObserver(context)
        assertTrue(observer.canObserve(context.packageName))
    }
}
