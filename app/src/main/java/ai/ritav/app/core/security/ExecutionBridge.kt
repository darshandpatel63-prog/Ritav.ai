package ai.ritav.app.core.security

/** Only approved Android adapters implement this interface. */
interface AndroidActionAdapter {
    fun execute(plan: ActionPlan): ExecutionResult
}

data class ExecutionResult(
    val success: Boolean,
    val verified: Boolean,
    val message: String
)

/** Final execution boundary. No adapter execution occurs before the full security pipeline passes. */
class ExecutionBridge(
    private val capabilityPolicyGate: CapabilityPolicyGate,
    private val securityPipeline: SecurityExecutionPipeline,
    private val adapter: AndroidActionAdapter,
    private val resultVerifier: ResultVerifier = ResultVerifier(),
    private val auditLog: AuditLog = securityPipeline.auditLog,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun execute(
        plan: ActionPlan,
        userExplicitlyRequested: Boolean,
        authorizationLevel: AuthorizationLevel = AuthorizationLevel.NONE,
        containsSensitiveData: Boolean = false,
        authorizationToken: String? = null,
        identitySession: SecuritySession? = null,
        inputText: String? = null
    ): ExecutionResult {
        val now = clock()
        val actionHash = plan.stableHash()
        val action = ActionRequest(
            appId = plan.appId,
            action = plan.action,
            riskTier = plan.riskTier,
            capability = plan.capability,
            sessionId = plan.sessionId,
            containsSensitiveData = containsSensitiveData,
            userExplicitlyRequested = userExplicitlyRequested,
            authorizationLevel = authorizationLevel
        )

        val capabilityDecision = capabilityPolicyGate.evaluate(action)
        if (!capabilityDecision.allowed) {
            auditLog.append(AuditEvent(now, plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
                false, false, capabilityDecision.reason))
            return ExecutionResult(false, false, capabilityDecision.reason)
        }

        val securityDecision = securityPipeline.authorize(
            SecurityExecutionRequest(
                action = action,
                plan = plan,
                authorizationToken = authorizationToken,
                identitySession = identitySession,
                nowEpochMillis = now,
                inputText = inputText
            )
        )
        if (!securityDecision.allowed) {
            auditLog.append(AuditEvent(now, plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
                false, false, securityDecision.reason))
            return ExecutionResult(false, false, securityDecision.reason)
        }

        auditLog.append(AuditEvent(now, plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
            true, false, "Capability and security pipeline checks passed"))

        val adapterResult = runCatching { adapter.execute(plan) }.getOrElse {
            auditLog.append(AuditEvent(now, plan.sessionId, actionHash, AuditEventType.EXECUTION,
                false, false, "Adapter execution failed"))
            auditLog.append(AuditEvent(now, plan.sessionId, actionHash, AuditEventType.VERIFICATION,
                false, false, "Result verification failed"))
            return ExecutionResult(false, false, "Action execution failed")
        }

        auditLog.append(AuditEvent(now, plan.sessionId, actionHash, AuditEventType.EXECUTION,
            adapterResult.success, false,
            if (adapterResult.success) "Adapter execution succeeded" else "Adapter execution failed"))

        val verification = resultVerifier.verify(
            expectedSuccess = true,
            evidence = ActionResultEvidence(
                success = adapterResult.success,
                errorCode = if (adapterResult.success) null else "ADAPTER_EXECUTION_FAILED"
            )
        )
        auditLog.append(AuditEvent(now, plan.sessionId, actionHash, AuditEventType.VERIFICATION,
            adapterResult.success, verification.verified,
            if (verification.verified) "Result verification passed" else "Result verification failed"))

        return ExecutionResult(
            success = adapterResult.success && verification.verified,
            verified = verification.verified,
            message = verification.reason
        )
    }
}
