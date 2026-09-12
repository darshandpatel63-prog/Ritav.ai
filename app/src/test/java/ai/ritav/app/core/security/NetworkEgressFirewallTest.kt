package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkEgressFirewallTest {
    private val firewall = NetworkEgressFirewall()

    @Test fun secretsAreAlwaysDenied() {
        val result = firewall.evaluate(NetworkEgressRequest("https://example.invalid", "sync", DataClassification.SECRET, true))
        assertFalse(result.allowed)
    }

    @Test fun sensitiveDataNeedsExplicitAuthorization() {
        val denied = firewall.evaluate(NetworkEgressRequest("https://example.invalid", "sync", DataClassification.SENSITIVE))
        assertFalse(denied.allowed)
        val allowed = firewall.evaluate(NetworkEgressRequest("https://example.invalid", "sync", DataClassification.SENSITIVE, true))
        assertTrue(allowed.allowed)
    }

    @Test fun publicDataCanPassClassificationBoundary() {
        val result = firewall.evaluate(NetworkEgressRequest("https://example.invalid", "fetch public resource", DataClassification.PUBLIC))
        assertTrue(result.allowed)
    }
}
