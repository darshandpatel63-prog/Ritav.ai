package ai.ritav.app.platform

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.SensitiveInformationFirewall
import ai.ritav.app.core.security.UntrustedContent

/**
 * Process-local bridge between the user-enabled AccessibilityService and the
 * deterministic execution/result-verification and model-context boundaries.
 *
 * Raw AccessibilityEvent/AccessibilityNodeInfo instances never leave the
 * service boundary. Semantic evidence keeps only package/type/time; model
 * context is separately retained for one-shot consumption only after an
 * additional deterministic secret inspection.
 */
internal object AndroidAccessibilityEvidenceBroker {
    internal data class SemanticEvidence(
        val packageName: String,
        val eventType: Int,
        val eventTimeElapsedMillis: Long,
        val receivedAtElapsedMillis: Long
    )

    private const val SOURCE_PREFIX = "android-accessibility:"
    private const val MAX_CONTEXT_LENGTH = 8_192
    private const val MAX_SEMANTIC_WINDOW_MILLIS = 2_000L
    private const val MAX_PACKAGE_NAME_LENGTH = 256

    private val lock = Any()
    private val sensitiveFirewall = SensitiveInformationFirewall()

    private var serviceConnected = false
    private var armedPackage: String? = null
    private var latestSemanticEvidence: SemanticEvidence? = null
    private var latestContext: UntrustedContent? = null

    fun setServiceConnected(connected: Boolean) {
        synchronized(lock) {
            serviceConnected = connected
            if (!connected) {
                armedPackage = null
                latestSemanticEvidence = null
                latestContext = null
            }
        }
    }

    fun isServiceConnected(): Boolean = synchronized(lock) { serviceConnected }

    fun arm(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) return false
        synchronized(lock) {
            if (!serviceConnected) return false
            armedPackage = packageName
            latestSemanticEvidence = null
            latestContext = null
            return true
        }
    }

    fun disarm() {
        synchronized(lock) {
            armedPackage = null
            latestSemanticEvidence = null
            latestContext = null
        }
    }

    fun isArmedFor(packageName: String): Boolean =
        synchronized(lock) { serviceConnected && armedPackage == packageName }

    fun recordSemanticEvent(
        event: AccessibilityEvent,
        receivedAtElapsedMillis: Long,
        root: AccessibilityNodeInfo?
    ) {
        if (receivedAtElapsedMillis < 0L || event.eventTime < 0L) return
        val packageName = event.packageName?.toString() ?: return
        if (!isValidPackageName(packageName)) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        synchronized(lock) {
            if (!serviceConnected || armedPackage != packageName) return
            if (root?.packageName?.toString() != packageName) return
            latestSemanticEvidence = SemanticEvidence(
                packageName = packageName,
                eventType = event.eventType,
                eventTimeElapsedMillis = event.eventTime,
                receivedAtElapsedMillis = receivedAtElapsedMillis
            )
        }
    }

    fun observeSemanticEvidenceAfter(
        packageName: String,
        dispatchCompletedAtElapsedMillis: Long,
        nowElapsedMillis: Long
    ): Boolean {
        if (!isValidPackageName(packageName) ||
            dispatchCompletedAtElapsedMillis < 0L ||
            nowElapsedMillis < dispatchCompletedAtElapsedMillis
        ) return false
        synchronized(lock) {
            val evidence = latestSemanticEvidence ?: return false
            if (evidence.packageName != packageName) return false
            if (evidence.eventTimeElapsedMillis <= dispatchCompletedAtElapsedMillis) return false
            if (evidence.receivedAtElapsedMillis <= dispatchCompletedAtElapsedMillis) return false
            if (evidence.eventTimeElapsedMillis > nowElapsedMillis) return false
            if (evidence.receivedAtElapsedMillis > nowElapsedMillis) return false
            if (dispatchCompletedAtElapsedMillis > Long.MAX_VALUE - MAX_SEMANTIC_WINDOW_MILLIS) return false
            val deadline = dispatchCompletedAtElapsedMillis + MAX_SEMANTIC_WINDOW_MILLIS
            return evidence.eventTimeElapsedMillis <= deadline &&
                evidence.receivedAtElapsedMillis <= deadline
        }
    }

    fun publishModelContext(content: UntrustedContent): Boolean {
        val packageName = content.source.removePrefix(SOURCE_PREFIX)
        if (!isValidPackageName(packageName)) return false
        if (content.text.isBlank() || content.text.length > MAX_CONTEXT_LENGTH) return false
        if (content.trustLevel != ContentTrustLevel.APP_CONTENT) return false

        val inspected = runCatching { sensitiveFirewall.inspect(content.text) }.getOrNull() ?: return false
        if (!inspected.allowed) return false

        synchronized(lock) {
            if (!serviceConnected || armedPackage != packageName) return false
            latestContext = content.copy(text = inspected.redactedText)
            return true
        }
    }

    fun consumeModelContext(packageName: String): UntrustedContent? {
        if (!isValidPackageName(packageName)) return null
        synchronized(lock) {
            if (armedPackage != packageName) return null
            val value = latestContext
            latestContext = null
            return value
        }
    }

    private fun isValidPackageName(packageName: String): Boolean =
        packageName.length <= MAX_PACKAGE_NAME_LENGTH &&
            PACKAGE_NAME_REGEX.matches(packageName)
}
