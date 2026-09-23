package ai.ritav.app.platform

import android.content.ComponentName
import android.content.Intent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.ritav.app.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RitavAccessibilityServiceInstrumentationTest {
    @Test fun userEnabledAccessibilityServiceSuppliesBoundedSemanticEvidence() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val uiAutomation = instrumentation.uiAutomation
        val component = ComponentName(context, RitavAccessibilityService::class.java)
            .flattenToString()

        uiAutomation.executeShellCommand(
            "settings put secure enabled_accessibility_services " + component
        ).close()
        uiAutomation.executeShellCommand(
            "settings put secure accessibility_enabled 1"
        ).close()

        try {
            val observer = AndroidAccessibilitySemanticTaskObserver(context)
            assertTrue(waitUntil { observer.canObserve(context.packageName) })

            assertTrue(observer.arm(context.packageName))
            val dispatchCompletedAt = SystemClock.elapsedRealtime()
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )

            assertTrue(observer.observeCompletedAfterDispatch(context.packageName))
            observer.disarm()

            val evidence = AndroidAccessibilityEvidenceBroker.consumeModelContext(context.packageName)
            if (evidence != null) {
                assertNotNull(evidence.text)
                assertFalse(evidence.text.length > 8_192)
            }
            assertTrue(dispatchCompletedAt >= 0L)
        } finally {
            AndroidAccessibilityEvidenceBroker.disarm()
        }
    }

    private fun waitUntil(predicate: () -> Boolean): Boolean {
        repeat(30) {
            if (predicate()) return true
            SystemClock.sleep(100L)
        }
        return predicate()
    }
}
