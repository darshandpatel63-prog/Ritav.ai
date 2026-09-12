package ai.ritav.app.core.security

/** Security-only orchestration boundary for consequential actions. */
data class SecurityExecutionRequest(
    val action: ActionRequest,
    val plan: ActionPlan,
    val authorizationToken: String? = null,
    val identitySession: SecuritySession? = null,
    val nowEpochMillis: Long,
    val inputText: String? = null
)

data class SecurityExecutionDecision(
    val allowed: Boolean,
    val reason: String,
    val authorizationRequired: AuthorizationLevel,
    val sanitizedInput: String? = null
)

class SecurityExecutionPipeline(
    private val policyEngine: PolicyEngine,
    private val executionPolicyGate: ExecutionPolicyGate,
    private val authorizationGate: ActionAuthorizationGate,
    private val identitySessionManager: IdentitySessionManager = IdentitySessionManager(),
    private val auditLog: AuditLog = InMemoryAuditLog(),
    private val sensitiveFirewall: SensitiveInformationFirewall = SensitiveInformationFirewall()
) {
    fun authorize(request: SecurityExecutionRequest): SecurityExecutionDecision {
        val actionHash = request.plan.stableHash()

        if (request.plan.appId != request.action.appId ||
            request.plan.capability != request.action.capability ||
            request.plan.action != request.action.action ||
            request.plan.riskTier != request.action.riskTier ||
            request.plan.sessionId != request.action.sessionId
        ) {
            return denyAndAudit(request, actionHash, "Action plan does not match execution request", AuthorizationLevel.NONE)
        }

        val inspectedInput = request.inputText?.let(sensitiveFirewall::inspect)
        if (inspectedInput?.matches?.isNotEmpty() == true) {
            return denyAndAudit(
                request, actionHash,
                "Sensitive information detected; input is blocked before execution",
                AuthorizationLevel.NONE
            ).copy(sanitizedInput = inspectedInput.redactedText)
        }

        if (request.action.containsSensitiveData) {
            return denyAndAudit(request, actionHash, "Sensitive data cannot enter action reasoning", AuthorizationLevel.NONE)
        }

        if (request.action.riskTier >= RiskTier.TIER_2_CONTENT_MUTATION &&
            !identitySessionManager.permitsProtectedCapability(request.identitySession, request.nowEpochMillis)
        ) {
            return denyAndAudit(request, actionHash, "Protected action requires an active trusted identity session", AuthorizationLevel.USER_CONFIRMATION)
        }

        val decision = executionPolicyGate.authorize(request.action)
        if (!decision.allowed) {
            return denyAndAudit(request, actionHash, decision.reason, decision.requiredAuthorization)
        }

        val required = decision.requiredAuthorization
        if (required != AuthorizationLevel.NONE) {
            val token = request.authorizationToken
                ?: return denyAndAudit(request, actionHash, "One-time authorization token is required", required)
            if (!authorizationGate.consume(token, request.plan, request.action.authorizationLevel, request.nowEpochMillis)) {
                return denyAndAudit(request, actionHash, "Authorization token is invalid, expired, mismatched, or already consumed", required)
            }
            auditLog.append(AuditEvent(
                request.nowEpochMillis, request.action.sessionId, actionHash,
                AuditEventType.AUTHORIZATION, true, false, "One-time authorization accepted"
            ))
        }

        auditLog.append(AuditEvent(
            request.nowEpochMillis, request.action.sessionId, actionHash,
            AuditEventType.POLICY_DECISION, true, false, decision.reason
        ))
        return SecurityExecutionDecision(true, decision.reason, required, inspectedInput?.redactedText ?: request.inputText)
    }

    fun audit(): List<AuditEvent> = auditLog.readAll()

    private fun denyAndAudit(
        request: SecurityExecutionRequest,
        actionHash: String,
        reason: String,
        required: AuthorizationLevel
    ): SecurityExecutionDecision {
        auditLog.append(AuditEvent(
            request.nowEpochMillis, request.action.sessionId, actionHash,
            AuditEventType.POLICY_DECISION, false, false, reason
        ))
        return SecurityExecutionDecision(false, reason, required)
    }
}
