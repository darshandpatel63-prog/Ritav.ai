package ai.ritav.app.platform

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

/**
 * Minimal usage-event representation used by the target-app observation boundary.
 *
 * The observer intentionally retains only the target package, event type and
 * event timestamp. UI text, accessibility nodes, class names and event extras
 * are never exposed to the security/result-verification path.
 */
internal data class AndroidUsageEventSnapshot(
    val packageName: String,
    val eventType: Int,
    val timestampMillis: Long
)

internal fun interface AndroidUsageEventReader {
    /**
     * Returns only events for [packageName].
     *
     * Null means the usage-event source is unavailable/inaccessible and must
     * therefore fail closed. An empty list is a successful query with no match.
     */
    fun query(
        packageName: String,
        beginMillis: Long,
        endMillis: Long
    ): List<AndroidUsageEventSnapshot>?
}

/**
 * Android framework-backed usage-event reader.
 *
 * This reader is intentionally package-filtered and bounded. The underlying
 * UsageEvents stream may contain other packages, but only matching package
 * identity plus event type/timestamp are retained.
 */
internal class ContextAndroidUsageEventReader(
    context: Context
) : AndroidUsageEventReader {
    private val usageStatsManager =
        context.getSystemService(UsageStatsManager::class.java)

    override fun query(
        packageName: String,
        beginMillis: Long,
        endMillis: Long
    ): List<AndroidUsageEventSnapshot>? {
        if (!isValidPackageName(packageName) || beginMillis < 0L || endMillis <= beginMillis) {
            return emptyList()
        }

        return runCatching {
            val usageEvents =
                usageStatsManager?.queryEvents(beginMillis, endMillis)
                    ?: return@runCatching null

            val event = UsageEvents.Event()
            val snapshots = ArrayList<AndroidUsageEventSnapshot>()
            var inspectedEvents = 0

            while (usageEvents.hasNextEvent()) {
                inspectedEvents += 1
                if (inspectedEvents > MAX_EVENTS_PER_QUERY) {
                    return@runCatching null
                }
                if (!usageEvents.getNextEvent(event)) {
                    break
                }

                val eventPackage = event.packageName ?: continue
                if (eventPackage != packageName) {
                    continue
                }

                snapshots += AndroidUsageEventSnapshot(
                    packageName = eventPackage,
                    eventType = event.eventType,
                    timestampMillis = event.timeStamp
                )
            }

            snapshots
        }.getOrNull()
    }

    private companion object {
        const val MAX_EVENTS_PER_QUERY = 128
        const val MAX_PACKAGE_NAME_LENGTH = 256

        fun isValidPackageName(packageName: String): Boolean =
            packageName.isNotBlank() && packageName.length <= MAX_PACKAGE_NAME_LENGTH
    }
}

/**
 * Independent target-app observation used by the Android APP_LAUNCH + open path.
 *
 * This does not inspect UI contents. It verifies only that the exact target
 * package produced a foreground activity event after launch dispatch.
 */
internal data class AndroidForegroundObservation(
    val packageName: String,
    val observedAtMillis: Long
)

internal interface AndroidTargetAppResultObserver {
    fun canObserve(packageName: String): Boolean

    fun observeForegroundAfterDispatch(
        packageName: String,
        dispatchStartedAtMillis: Long
    ): AndroidForegroundObservation?
}

internal class AndroidTargetAppForegroundObserver(
    private val eventReader: AndroidUsageEventReader,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) }
) : AndroidTargetAppResultObserver {

    constructor(
        context: Context
    ) : this(
        eventReader = ContextAndroidUsageEventReader(context.applicationContext)
    )

    override fun canObserve(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) return false
        val now = safeNow() ?: return false
        if (now <= 0L) return false

        val begin = (now - PROBE_WINDOW_MILLIS).coerceAtLeast(0L)
        return runCatching {
            eventReader.query(packageName, begin, now) != null
        }.getOrDefault(false)
    }

    override fun observeForegroundAfterDispatch(
        packageName: String,
        dispatchStartedAtMillis: Long
    ): AndroidForegroundObservation? {
        if (!isValidPackageName(packageName)) return null
        if (dispatchStartedAtMillis < 0L) return null
        if (dispatchStartedAtMillis > Long.MAX_VALUE - MAX_WAIT_MILLIS) return null

        val deadline = dispatchStartedAtMillis + MAX_WAIT_MILLIS

        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            val now = safeNow() ?: return null
            if (now < dispatchStartedAtMillis) return null

            val queryEnd = minOf(now, deadline)
                .let { end -> end + 1L }

            if (queryEnd > dispatchStartedAtMillis) {
                val events = runCatching {
                    eventReader.query(
                        packageName = packageName,
                        beginMillis = dispatchStartedAtMillis,
                        endMillis = queryEnd
                    )
                }.getOrNull() ?: return null

                if (events.size > MAX_EVENTS_PER_QUERY) {
                    return null
                }

                val matchingEvent = events.firstOrNull { event ->
                    event.packageName == packageName &&
                        event.timestampMillis > dispatchStartedAtMillis &&
                        event.timestampMillis <= deadline &&
                        event.timestampMillis < queryEnd &&
                        isForegroundEvent(event.eventType)
                }
                if (matchingEvent != null) {
                    return AndroidForegroundObservation(
                        packageName = packageName,
                        observedAtMillis = matchingEvent.timestampMillis
                    )
                }
            }

            if (now >= deadline || attempt == MAX_POLL_ATTEMPTS - 1) {
                return null
            }

            if (!runCatching { sleeper(POLL_INTERVAL_MILLIS) }.isSuccess) {
                return null
            }
        }

        return null
    }

    private fun safeNow(): Long? =
        runCatching { clock() }
            .getOrNull()
            ?.takeIf { it >= 0L }

    private fun isForegroundEvent(eventType: Int): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }

    private companion object {
        const val MAX_PACKAGE_NAME_LENGTH = 256
        const val PROBE_WINDOW_MILLIS = 1_000L
        const val MAX_WAIT_MILLIS = 2_000L
        const val POLL_INTERVAL_MILLIS = 100L
        const val MAX_POLL_ATTEMPTS = 21
        const val MAX_EVENTS_PER_QUERY = 128

        fun isValidPackageName(packageName: String): Boolean =
            packageName.isNotBlank() && packageName.length <= MAX_PACKAGE_NAME_LENGTH
    }
}
