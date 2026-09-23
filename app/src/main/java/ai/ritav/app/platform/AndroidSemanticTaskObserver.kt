package ai.ritav.app.platform

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/**
 * Accessibility-backed semantic result observation.
 *
 * The current executable scope is APP_LAUNCH + open: completion requires a
 * post-dispatch target-package window-state/content event with a matching
 * active-window package. It does not claim arbitrary in-app task completion.
 */
internal interface AndroidSemanticTaskObserver {
    fun canObserve(packageName: String): Boolean
    fun arm(packageName: String): Boolean
    fun observeCompletedAfterDispatch(
        packageName: String,
        dispatchCompletedAtElapsedMillis: Long
    ): Boolean
    fun disarm()
}

internal class AndroidAccessibilitySemanticTaskObserver(
    private val context: Context,
    private val broker: AndroidAccessibilityEvidenceBroker = AndroidAccessibilityEvidenceBroker,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) }
) : AndroidSemanticTaskObserver {

    override fun canObserve(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) return false
        return isAccessibilityServiceEnabled() && broker.isServiceConnected()
    }

    override fun arm(packageName: String): Boolean {
        if (!canObserve(packageName)) return false
        val service = RitavAccessibilityService.currentService() ?: return false
        if (!broker.arm(packageName)) return false
        val restricted = runCatching { service.restrictToPackage(packageName) }.getOrDefault(false)
        if (!restricted) {
            broker.disarm()
            service.restrictToRitavOnly()
        }
        return restricted
    }

    override fun observeCompletedAfterDispatch(
        packageName: String,
        dispatchCompletedAtElapsedMillis: Long
    ): Boolean {
        if (!isValidPackageName(packageName) || dispatchCompletedAtElapsedMillis < 0L) return false
        if (dispatchCompletedAtElapsedMillis > Long.MAX_VALUE - MAX_WAIT_MILLIS) return false
        val deadline = dispatchCompletedAtElapsedMillis + MAX_WAIT_MILLIS

        return try {
            repeat(MAX_POLL_ATTEMPTS) { attempt ->
                val now = safeNow() ?: return false
                if (now < dispatchCompletedAtElapsedMillis) return false

                if (now <= deadline && broker.observeSemanticEvidenceAfter(
                        packageName = packageName,
                        dispatchCompletedAtElapsedMillis = dispatchCompletedAtElapsedMillis
                    )
                ) {
                    return true
                }

                if (now >= deadline || attempt == MAX_POLL_ATTEMPTS - 1) return false
                if (!runCatching { sleeper(POLL_INTERVAL_MILLIS) }.isSuccess) return false
            }
            false
        } finally {
            disarm()
        }
    }

    override fun disarm() {
        broker.disarm()
        RitavAccessibilityService.currentService()?.restrictToRitavOnly()
    }

    private fun safeNow(): Long? =
        runCatching { clock() }.getOrNull()?.takeIf { it >= 0L }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = context.packageName + "/" + RitavAccessibilityService::class.java.name
        return enabled.split(':').any { it == expected }
    }

    private fun isValidPackageName(packageName: String): Boolean =
        packageName.length <= MAX_PACKAGE_NAME_LENGTH &&
            PACKAGE_NAME_REGEX.matches(packageName)

    private companion object {
        const val MAX_PACKAGE_NAME_LENGTH = 256
        val PACKAGE_NAME_REGEX = Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$""")
        const val MAX_WAIT_MILLIS = 2_000L
        const val POLL_INTERVAL_MILLIS = 100L
        const val MAX_POLL_ATTEMPTS = 21
    }
}
