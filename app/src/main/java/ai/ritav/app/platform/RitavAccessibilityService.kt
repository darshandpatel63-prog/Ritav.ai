package ai.ritav.app.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * User-enabled assistive service. It never executes actions. While idle it is
 * restricted to Ritav's own package. A requested external target is scoped only
 * for the active observation window.
 */
internal class RitavAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        current = this
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
        restrictToRitavOnly()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return
        if (!AndroidAccessibilityEvidenceBroker.isArmedFor(packageName)) return

        val root = runCatching { rootInActiveWindow }.getOrNull()
        if (root?.packageName?.toString() != packageName) return

        val now = SystemClock.elapsedRealtime()
        AndroidAccessibilityEvidenceBroker.recordSemanticEvent(event, now, root)

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            runCatching {
                RitavAccessibilityContextExtractor().extract(root, packageName)
            }.getOrNull()?.let { context ->
                AndroidAccessibilityEvidenceBroker.publishModelContext(context)
            }
        }
    }

    override fun onInterrupt() {
        AndroidAccessibilityEvidenceBroker.disarm()
    }

    override fun onDestroy() {
        AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
        current = null
        super.onDestroy()
    }

    fun restrictToPackage(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) return false
        return runCatching {
            val info = serviceInfo
            info.packageNames = arrayOf(packageName)
            serviceInfo = info
            true
        }.getOrDefault(false)
    }

    fun restrictToRitavOnly(): Boolean =
        runCatching {
            val info = serviceInfo
            info.packageNames = arrayOf(applicationContext.packageName)
            serviceInfo = info
            true
        }.getOrDefault(false)

    private fun isValidPackageName(packageName: String): Boolean =
        packageName.isNotBlank() && packageName.length <= 256

    companion object {
        @Volatile private var current: RitavAccessibilityService? = null
        fun currentService(): RitavAccessibilityService? = current
    }
}
