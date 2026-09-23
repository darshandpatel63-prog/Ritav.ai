package ai.ritav.app.platform

import android.accessibilityservice.AccessibilityService
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
        if (!restrictToRitavOnly()) {
            current = null
            AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
            disableSelf()
            return
        }
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return
        if (!AndroidAccessibilityEvidenceBroker.isArmedFor(packageName)) return

        val root = runCatching { rootInActiveWindow }.getOrNull()
        if (root == null || root.packageName?.toString() != packageName) return

        try {
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
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() {
        AndroidAccessibilityEvidenceBroker.disarm()
        restrictToRitavOnly()
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
        packageName.length <= MAX_PACKAGE_NAME_LENGTH &&
            PACKAGE_NAME_REGEX.matches(packageName)

    private companion object {
        const val MAX_PACKAGE_NAME_LENGTH = 256
        val PACKAGE_NAME_REGEX = Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$""")
    }

    companion object {
        @Volatile private var current: RitavAccessibilityService? = null
        fun currentService(): RitavAccessibilityService? = current
    }
}
