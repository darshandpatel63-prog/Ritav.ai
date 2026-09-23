package ai.ritav.app.platform

import ai.ritav.app.core.orchestrator.ScreenContentSource
import ai.ritav.app.core.orchestrator.ScreenContextSnapshot
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * OS-bound screen-content producer.
 *
 * The system starts this service only after the user explicitly enables it in
 * Accessibility Settings. The service stays dormant until an in-memory
 * security-validated task is armed.
 */
class RitavAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val runtime = AndroidAccessibilityRuntimeRegistry.current() ?: return
        val eventPackage = event?.packageName?.toString() ?: return
        if (eventPackage.isBlank() || eventPackage.length > MAX_PACKAGE_NAME_LENGTH) return

        val now = SystemClock.elapsedRealtime()
        if (now < 0L) return
        val taskId = runtime.gate.currentTaskFor(
            packageName = eventPackage,
            nowElapsedRealtime = now,
            currentStopGeneration = runtime.currentStopGeneration()
        ) ?: return

        val root = runCatching { getRootInActiveWindow() }.getOrNull() ?: return
        try {
            val text = extractVisibleText(root, eventPackage) ?: return
            val snapshot = ScreenContextSnapshot(
                taskId = taskId,
                packageName = eventPackage,
                source = ScreenContentSource.ACCESSIBILITY,
                text = text,
                observedAtMillis = now
            )
            runtime.bridge.consume(
                snapshot = snapshot,
                nowElapsedRealtime = now,
                currentStopGeneration = runtime.currentStopGeneration()
            )
        } finally {
            runCatching { root.recycle() }
        }
    }

    override fun onInterrupt() {
        AndroidAccessibilityRuntimeRegistry.current()?.gate?.disarm()
    }

    override fun onDestroy() {
        AndroidAccessibilityRuntimeRegistry.current()?.gate?.disarm()
        super.onDestroy()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: return
        info.eventTypes =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.notificationTimeout = 100L
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo = info
    }

    private fun extractVisibleText(root: AccessibilityNodeInfo, targetPackage: String): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        val parts = LinkedHashSet<String>()
        var inspectedNodes = 0

        while (queue.isNotEmpty() && inspectedNodes < MAX_NODES) {
            val node = queue.removeFirst()
            inspectedNodes++

            val nodePackage = node.packageName?.toString()
            if (nodePackage != targetPackage) continue

            if (node.isVisibleToUser && !node.isPassword && !node.isEditable) {
                appendText(parts, node.text)
                appendText(parts, node.contentDescription)
                if (parts.sumOf { it.length } >= MAX_TEXT_LENGTH) break
            }

            val childCount = node.childCount.coerceAtMost(MAX_CHILDREN_PER_NODE)
            for (index in 0 until childCount) {
                val child = runCatching { node.getChild(index) }.getOrNull() ?: continue
                if (queue.size < MAX_NODES) queue.addLast(child)
            }
        }

        return parts.joinToString("\n")
            .take(MAX_TEXT_LENGTH)
            .takeIf { it.isNotBlank() }
    }

    private fun appendText(parts: MutableSet<String>, value: CharSequence?) {
        val text = value?.toString()?.trim() ?: return
        if (text.isBlank() || text.length > MAX_NODE_TEXT_LENGTH) return
        parts.add(text)
    }

    private companion object {
        const val MAX_PACKAGE_NAME_LENGTH = 256
        const val MAX_NODES = 256
        const val MAX_CHILDREN_PER_NODE = 32
        const val MAX_NODE_TEXT_LENGTH = 2_048
        const val MAX_TEXT_LENGTH = 16_384
    }
}