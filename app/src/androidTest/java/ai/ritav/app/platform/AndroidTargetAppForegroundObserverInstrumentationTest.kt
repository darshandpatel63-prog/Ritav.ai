package ai.ritav.app.platform

import android.content.Intent
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.ritav.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidTargetAppForegroundObserverInstrumentationTest {
    @Test fun usageStatsReaderCanObserveForegroundTransitionOnManagedDevice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        instrumentation.uiAutomation
            .executeShellCommand(
                "appops set " + context.packageName + " android:get_usage_stats allow"
            )
            .close()

        val observer = AndroidTargetAppForegroundObserver(context)
        assertTrue(observer.canObserve(context.packageName))

        val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val settingsPackage =
            context.packageManager.resolveActivity(
                settingsIntent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )?.activityInfo?.packageName
        assertTrue(settingsPackage != null)

        val dispatchStartedAtMillis = System.currentTimeMillis()
        context.startActivity(settingsIntent)

        val observation = observer.observeForegroundAfterDispatch(
            packageName = settingsPackage!!,
            dispatchStartedAtMillis = dispatchStartedAtMillis
        )
        assertTrue(observation != null)
        assertEquals(settingsPackage, observation?.packageName)
        assertTrue((observation?.observedAtMillis ?: -1L) >= dispatchStartedAtMillis)
    }
}
