package ai.ritav.app.platform

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.ritav.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidTargetAppForegroundObserverInstrumentationTest {
    @Test
    fun usageStatsReaderCanObserveForegroundTransitionOnManagedDevice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        instrumentation.uiAutomation
            .executeShellCommand(
                "appops set " + context.packageName + " android:get_usage_stats allow"
            )
            .close()

        val observer = AndroidTargetAppForegroundObserver(context)
        assertTrue(observer.canObserve(context.packageName))

        instrumentation.uiAutomation
            .executeShellCommand("input keyevent KEYCODE_HOME")
            .close()
        Thread.sleep(200)

        val dispatchStartedAtMillis = System.currentTimeMillis()
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

        val observation = observer.observeForegroundAfterDispatch(
            packageName = context.packageName,
            dispatchStartedAtMillis = dispatchStartedAtMillis
        )
        assertTrue(observation != null)
        assertEquals(context.packageName, observation?.packageName)
        assertTrue((observation?.observedAtMillis ?: -1L) >= dispatchStartedAtMillis)
    }
}
