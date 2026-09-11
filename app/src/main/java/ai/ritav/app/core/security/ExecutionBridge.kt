package ai.ritav.app.core.security

/**
 * Only approved Android adapters implement this interface. AI/orchestrator code
 * must submit an ActionRequest; it never receives unrestricted device control.
 */
interface AndroidActionAdapter {
    fun execute(plan: ActionPlan): ExecutionResult
}

data class ExecutionResult(
    val success: Boolean,
    val verified: Boolean,
    val message: String
)

/**
 * Final execution boundary. Policy is evaluated immediately before execution,
 * then the adapter is invoked only when the deterministic policy allows it.
 */
class ExecutionBridge(
    private val policyEngine: PolicyEngine,
    private val adapter: AndroidActionAdapter
) {
    fun execute(
        plan: ActionPlan,
        userExplicitlyRequested: Boolean,
        authorizationLevel: AuthorizationLevel = AuthorizationLevel.NONE,
        containsSensitiveData: Boolean = false
    ): ExecutionResult {
        val request = ActionRequest(
            appId = plan.appId,
            action = plan.action,
            riskTier = plan.riskTier,
            capability = plan.capability,
            sessionId = plan.sessionId,
            containsSensitiveData = containsSensitiveData,
            userExplicitlyRequested = userExplicitlyRequested,
            authorizationLevel = authorizationLevel
        )
        val decision = policyEngine.evaluate(request)
        if (!decision.allowed) {
            return ExecutionResult(false, false, decision.reason)
        }
        return adapter.execute(plan)
    }
}
