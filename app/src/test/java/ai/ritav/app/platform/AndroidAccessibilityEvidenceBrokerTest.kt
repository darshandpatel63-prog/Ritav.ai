package ai.ritav.app.platform

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.UntrustedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAccessibilityEvidenceBrokerTest {
    @Test fun safeContextIsConsumedExactlyOnce() {
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
        try {
            assertTrue(AndroidAccessibilityEvidenceBroker.arm("com.example.safe"))

            val published = AndroidAccessibilityEvidenceBroker.publishModelContext(
                UntrustedContent(
                    text = "Settings",
                    source = "android-accessibility:com.example.safe",
                    trustLevel = ContentTrustLevel.APP_CONTENT
                )
            )
            assertTrue(published)

            val first = AndroidAccessibilityEvidenceBroker.consumeModelContext("com.example.safe")
            assertEquals("Settings", first?.text)
            assertNull(AndroidAccessibilityEvidenceBroker.consumeModelContext("com.example.safe"))
        } finally {
            AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
        }
    }


    @Test fun onlyOneSemanticObservationMayBeArmedAtATime() {
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
        try {
            assertTrue(AndroidAccessibilityEvidenceBroker.arm("com.example.safe"))
            assertFalse(AndroidAccessibilityEvidenceBroker.arm("com.example.other"))
        } finally {
            AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
        }
    }

    @Test fun accessibilityContextRequiresInternalSourcePrefix() {
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
        try {
            assertTrue(AndroidAccessibilityEvidenceBroker.arm("com.example.safe"))
            assertFalse(
                AndroidAccessibilityEvidenceBroker.publishModelContext(
                    UntrustedContent(
                        text = "Settings",
                        source = "com.example.safe",
                        trustLevel = ContentTrustLevel.APP_CONTENT
                    )
                )
            )
        } finally {
            AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
        }
    }

    @Test fun sensitiveContextIsDroppedBeforeBrokerRetention() {
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
        try {
            assertTrue(AndroidAccessibilityEvidenceBroker.arm("com.example.safe"))
            assertFalse(
                AndroidAccessibilityEvidenceBroker.publishModelContext(
                    UntrustedContent(
                        text = "Password is hunter2",
                        source = "android-accessibility:com.example.safe",
                        trustLevel = ContentTrustLevel.APP_CONTENT
                    )
                )
            )
            assertNull(AndroidAccessibilityEvidenceBroker.consumeModelContext("com.example.safe"))
        } finally {
            AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
        }
    }

    @Test fun invalidOrAuthoritySmugglingContextIsRejected() {
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
        try {
            assertTrue(AndroidAccessibilityEvidenceBroker.arm("com.example.safe"))
            assertFalse(
                AndroidAccessibilityEvidenceBroker.publishModelContext(
                    UntrustedContent(
                        text = "Open payment",
                        source = "android-accessibility:bad package",
                        trustLevel = ContentTrustLevel.APP_CONTENT
                    )
                )
            )
            assertFalse(
                AndroidAccessibilityEvidenceBroker.publishModelContext(
                    UntrustedContent(
                        text = "Open settings",
                        source = "android-accessibility:com.example.safe",
                        trustLevel = ContentTrustLevel.USER_COMMAND
                    )
                )
            )
        } finally {
            AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
        }
    }


    @Test fun semanticEvidenceRequiresPostDispatchSourceAndReceiptTimes() {
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
        try {
            assertTrue(AndroidAccessibilityEvidenceBroker.arm("com.example.safe"))
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            event.packageName = "com.example.safe"
            event.eventTime = 200L
            val root = AccessibilityNodeInfo.obtain()
            root.packageName = "com.example.safe"
            AndroidAccessibilityEvidenceBroker.recordSemanticEvent(
                event = event,
                receivedAtElapsedMillis = 250L,
                root = root
            )
            assertTrue(
                AndroidAccessibilityEvidenceBroker.observeSemanticEvidenceAfter(
                    packageName = "com.example.safe",
                    dispatchCompletedAtElapsedMillis = 100L,
                    nowElapsedMillis = 300L
                )
            )
            root.recycle()
            event.recycle()
        } finally {
            AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
        }
    }

    @Test fun staleAccessibilityEventCannotBecomePostDispatchEvidence() {
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
        try {
            assertTrue(AndroidAccessibilityEvidenceBroker.arm("com.example.safe"))
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            event.packageName = "com.example.safe"
            event.eventTime = 90L
            val root = AccessibilityNodeInfo.obtain()
            root.packageName = "com.example.safe"
            AndroidAccessibilityEvidenceBroker.recordSemanticEvent(
                event = event,
                receivedAtElapsedMillis = 200L,
                root = root
            )
            assertFalse(
                AndroidAccessibilityEvidenceBroker.observeSemanticEvidenceAfter(
                    packageName = "com.example.safe",
                    dispatchCompletedAtElapsedMillis = 100L,
                    nowElapsedMillis = 300L
                )
            )
            root.recycle()
            event.recycle()
        } finally {
            AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
        }
    }

    @Test fun semanticEvidenceRequiresExactArmedPackageAndMatchingRoot() {
        AndroidAccessibilityEvidenceBroker.setServiceConnected(true)
        try {
            assertTrue(AndroidAccessibilityEvidenceBroker.arm("com.example.safe"))
            val event = AccessibilityEvent.obtain(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            )
            event.packageName = "com.example.other"
            AndroidAccessibilityEvidenceBroker.recordSemanticEvent(
                event = event,
                receivedAtElapsedMillis = 200L,
                root = null
            )
            assertFalse(
                AndroidAccessibilityEvidenceBroker.observeSemanticEvidenceAfter(
                    packageName = "com.example.safe",
                    dispatchCompletedAtElapsedMillis = 100L,
                    nowElapsedMillis = 200L
                )
            )
            event.recycle()
        } finally {
            AndroidAccessibilityEvidenceBroker.setServiceConnected(false)
        }
    }
}
