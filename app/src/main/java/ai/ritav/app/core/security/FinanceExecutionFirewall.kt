package ai.ritav.app.core.security

/**
 * Dedicated hard boundary for financial execution.
 *
 * This layer is intentionally deny-only in the current foundation: no caller,
 * authorization token, or user-intent flag can turn a financial capability into
 * an executable action. Existing PolicyEngine and CapabilityPolicyGate checks
 * remain in force; this firewall is an additional security choke point.
 */
internal class FinanceExecutionFirewall {
    fun inspect(request: ActionRequest): FinancialFirewallDecision {
        if (request.capability == Capability.FINANCIAL_ACTION) {
            return FinancialFirewallDecision(
                allowed = false,
                reason = "Financial execution is blocked by the dedicated finance firewall"
            )
        }
        return FinancialFirewallDecision(allowed = true, reason = "Action is outside the financial execution boundary")
    }
}

internal data class FinancialFirewallDecision(
    val allowed: Boolean,
    val reason: String
)
