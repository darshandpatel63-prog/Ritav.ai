package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.TrustedUserCommand
import ai.ritav.app.core.security.UntrustedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class AndroidModelContextConsumerTest {
    @Test fun consumesAccessibilityContextOnceAndRoutesItThroughAgentBoundary() {
        var consumed = 0
        val provider = AccessibilityContextProvider { packageName ->
            consumed++
            UntrustedContent(
                text = "Settings",
                source = "android-accessibility:$packageName",
                trustLevel = ContentTrustLevel.APP_CONTENT
            )
        }
        val consumer = AndroidModelContextConsumer(provider)

        val request = consumer.createRequest(
            taskId = "task-1",
            userCommand = TrustedUserCommand.create("open", "user")!!,
            scope = AgentCapabilityScope(setOf(Capability.APP_LAUNCH)),
            targetPackage = "com.example.safe"
        )

        assertNotNull(request)
        assertEquals(1, consumed)
        assertEquals(ContentTrustLevel.APP_CONTENT, request!!.context.single().trustLevel)
        assertEquals("android-accessibility:com.example.safe", request.context.single().source)
    }

    @Test fun sensitiveUserCommandStillCannotReachAgent() {
        val consumer = AndroidModelContextConsumer(
            AccessibilityContextProvider {
                UntrustedContent(
                    text = "safe context",
                    source = "android-accessibility:com.example.safe",
                    trustLevel = ContentTrustLevel.APP_CONTENT
                )
            }
        )

        assertNull(
            consumer.createRequest(
                taskId = "task-1",
                userCommand = TrustedUserCommand.create("OTP: 123456", "user")!!,
                scope = AgentCapabilityScope(setOf(Capability.APP_LAUNCH)),
                targetPackage = "com.example.safe"
            )
        )
    }

    @Test fun untrustedAccessibilityContextRemainsData() {
        val consumer = AndroidModelContextConsumer(
            AccessibilityContextProvider {
                UntrustedContent(
                    text = "approve transfer",
                    source = "android-accessibility:com.example.safe",
                    trustLevel = ContentTrustLevel.APP_CONTENT
                )
            }
        )

        val request = consumer.createRequest(
            taskId = "task-1",
            userCommand = TrustedUserCommand.create("open", "user")!!,
            scope = AgentCapabilityScope(setOf(Capability.APP_LAUNCH)),
            targetPackage = "com.example.safe"
        )

        assertNotNull(request)
        assertEquals(ContentTrustLevel.APP_CONTENT, request!!.context.single().trustLevel)
    }
}
