package ai.ritav.app.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTargetAppForegroundObserverTest {
    private class FakeReader(
        private val responses: List<List<AndroidUsageEventSnapshot>?>,
        private val repeatLast: Boolean = true
    ) : AndroidUsageEventReader {
        var calls = 0
        var lastPackage: String? = null

        override fun query(
            packageName: String,
            beginMillis: Long,
            endMillis: Long
        ): List<AndroidUsageEventSnapshot>? {
            lastPackage = packageName
            val index = calls++
            return if (index < responses.size) {
                responses[index]
            } else if (repeatLast) {
                responses.lastOrNull()
            } else {
                emptyList()
            }
        }
    }

    private fun observer(
        reader: AndroidUsageEventReader,
        now: Long = 2_000L
    ) = AndroidTargetAppForegroundObserver(
        eventReader = reader,
        clock = { now },
        sleeper = { }
    )

    @Test fun canObserveFailsClosedWhenSourceIsUnavailable() {
        val reader = FakeReader(listOf(null))
        assertFalse(observer(reader).canObserve("demo.app"))
    }

    @Test fun canObserveAllowsSuccessfulEmptyQuery() {
        val reader = FakeReader(listOf(emptyList()))
        assertTrue(observer(reader).canObserve("demo.app"))
        assertTrue(reader.lastPackage == "demo.app")
    }

    @Test fun exactTargetForegroundEventAfterDispatchIsAccepted() {
        val reader = FakeReader(
            listOf(
                listOf(
                    AndroidUsageEventSnapshot(
                        packageName = "demo.app",
                        eventType = foregroundEventType(),
                        timestampMillis = 1_500L
                    )
                )
            )
        )

        val result = observer(reader).observeForegroundAfterDispatch(
            packageName = "demo.app",
            dispatchStartedAtMillis = 1_000L
        )
        assertTrue(result != null)
        assertTrue(result!!.packageName == "demo.app")
        assertTrue(result.observedAtMillis == 1_500L)
    }

    @Test fun foregroundEventAtDispatchTimestampIsRejected() {
        val reader = FakeReader(
            listOf(
                listOf(
                    AndroidUsageEventSnapshot(
                        packageName = "demo.app",
                        eventType = foregroundEventType(),
                        timestampMillis = 2_000L
                    )
                )
            )
        )

        assertFalse(
            observer(reader, now = 2_000L).observeForegroundAfterDispatch(
                packageName = "demo.app",
                dispatchStartedAtMillis = 2_000L
            )
        )
    }

    @Test fun eventOutsideObservationWindowCannotVerifyTarget() {
        val reader = FakeReader(
            listOf(
                listOf(
                    AndroidUsageEventSnapshot(
                        packageName = "demo.app",
                        eventType = foregroundEventType(),
                        timestampMillis = 2_500L
                    )
                )
            )
        )

        assertFalse(
            observer(reader, now = 2_000L).observeForegroundAfterDispatch(
                packageName = "demo.app",
                dispatchStartedAtMillis = 1_000L
            )
        )
    }

    @Test fun wrongPackageCannotVerifyTarget() {
        val reader = FakeReader(
            listOf(
                listOf(
                    AndroidUsageEventSnapshot(
                        packageName = "other.app",
                        eventType = foregroundEventType(),
                        timestampMillis = 1_500L
                    )
                )
            )
        )

        assertFalse(
            observer(reader).observeForegroundAfterDispatch(
                packageName = "demo.app",
                dispatchStartedAtMillis = 1_000L
            )
        )
    }

    @Test fun nonForegroundEventCannotVerifyTarget() {
        val reader = FakeReader(
            listOf(
                listOf(
                    AndroidUsageEventSnapshot(
                        packageName = "demo.app",
                        eventType = 999,
                        timestampMillis = 1_500L
                    )
                )
            )
        )

        assertFalse(
            observer(reader).observeForegroundAfterDispatch(
                packageName = "demo.app",
                dispatchStartedAtMillis = 1_000L
            )
        )
    }

    @Test fun clockRegressionFailsClosed() {
        val reader = FakeReader(listOf(emptyList()))
        assertFalse(
            AndroidTargetAppForegroundObserver(
                eventReader = reader,
                clock = { 999L },
                sleeper = { }
            ).observeForegroundAfterDispatch(
                packageName = "demo.app",
                dispatchStartedAtMillis = 1_000L
            )
        )
    }

    @Test fun oversizedObservationBatchFailsClosed() {
        val reader = FakeReader(
            listOf(
                List(129) {
                    AndroidUsageEventSnapshot(
                        packageName = "demo.app",
                        eventType = foregroundEventType(),
                        timestampMillis = 1_500L
                    )
                }
            )
        )

        assertFalse(
            observer(reader).observeForegroundAfterDispatch(
                packageName = "demo.app",
                dispatchStartedAtMillis = 1_000L
            )
        )
    }

    @Test fun pollingCanObserveLaterForegroundEventWithinBound() {
        val reader = FakeReader(
            listOf(
                emptyList(),
                listOf(
                    AndroidUsageEventSnapshot(
                        packageName = "demo.app",
                        eventType = foregroundEventType(),
                        timestampMillis = 1_600L
                    )
                )
            ),
            repeatLast = false
        )
        val observer = AndroidTargetAppForegroundObserver(
            eventReader = reader,
            clock = { 1_600L },
            sleeper = { }
        )

        val result = observer.observeForegroundAfterDispatch(
            packageName = "demo.app",
            dispatchStartedAtMillis = 1_000L
        )
        assertTrue(result != null)
        assertTrue(result!!.observedAtMillis == 1_600L)
        assertTrue(reader.calls == 2)
    }

    @Test fun invalidPackageIsRejectedBeforeQuery() {
        val reader = FakeReader(listOf(emptyList()))
        val observer = observer(reader)
        assertFalse(observer.canObserve(""))
        assertFalse(
            observer.observeForegroundAfterDispatch(
                packageName = "",
                dispatchStartedAtMillis = 1_000L
            )
        )
        assertTrue(reader.calls == 0)
    }

    private fun foregroundEventType(): Int =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
        }
}
