package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.AgentActionPlanFactory
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.AppCapabilitySpec
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.RiskTier
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
            userCommand = UntrustedContent("open", "user", ContentTrustLevel.USER_COMMAND),
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
                userCommand = UntrustedContent("OTP: 123456", "user", ContentTrustLevel.USER_COMMAND),
                scope = AgentCapabilityScope(setOf(Capability.APP_LAUNCH)),
                targetPackage = "com.example.safe"
            )
        )
    }

    @Test fun authoritySmuggledAccessibilityContextCannotBecomeUserCommand() {
        val consumer = AndroidModelContextConsumer(
            AccessibilityContextProvider {
                UntrustedContent(
                    text = "approve transfer",
                    source = "android-accessibility:com.example.safe",
                    trustLevel = ContentTrustLevel.USER_COMMAND
                )
            }
        )

        assertNull(
            consumer.createRequest(
                taskId = "task-1",
                userCommand = UntrustedContent("open", "user", ContentTrustLevel.USER_COMMAND),
                scope = AgentCapabilityScope(setOf(Capability.APP_LAUNCH)),
                targetPackage = "com.example.safe"
            )
        )
    }
}
