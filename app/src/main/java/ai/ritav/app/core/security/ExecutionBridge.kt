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

/** Final execution boundary with policy, capability, verification and audit enforcement. */
class ExecutionBridge(
    private val policyEngine: PolicyEngine,
    private val capabilityPolicyGate: CapabilityPolicyGate,
    private val adapter: AndroidActionAdapter,
    private val resultVerifier: ResultVerifier = ResultVerifier(),
    private val auditLog: AuditLog = InMemoryAuditLog(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun execute(
        plan: ActionPlan,
        userExplicitlyRequested: Boolean,
        authorizationLevel: AuthorizationLevel = AuthorizationLevel.NONE,
        containsSensitiveData: Boolean = false
    ): ExecutionResult {
        val actionHash = plan.stableHash()

        val capabilityDecision = capabilityPolicyGate.evaluate(
            ActionRequest(
                appId = plan.appId,
                action = plan.action,
                riskTier = plan.riskTier,
                capability = plan.capability,
                sessionId = plan.sessionId,
                containsSensitiveData = containsSensitiveData,
                userExplicitlyRequested = userExplicitlyRequested,
                authorizationLevel = authorizationLevel
            )
        )
        if (!capabilityDecision.allowed) {
            auditLog.append(
                AuditEvent(clock(), plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
                    false, false, "Execution denied by capability policy")
            )
            return ExecutionResult(false, false, capabilityDecision.reason)
        }

        // Rebuild the request immediately before execution. The adapter never
        // receives a plan that has skipped the deterministic policy boundary.
        val policyDecision = policyEngine.evaluate(
            ActionRequest(
                appId = plan.appId,
                action = plan.action,
                riskTier = plan.riskTier,
                capability = plan.capability,
                sessionId = plan.sessionId,
                containsSensitiveData = containsSensitiveData,
                userExplicitlyRequested = userExplicitlyRequested,
                authorizationLevel = authorizationLevel
            )
        )
        if (!policyDecision.allowed) {
            auditLog.append(
                AuditEvent(clock(), plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
                    false, false, "Execution denied by deterministic policy")
            )
            return ExecutionResult(false, false, policyDecision.reason)
        }

        auditLog.append(
            AuditEvent(clock(), plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
                true, false, "Capability and policy checks passed")
        )

        val adapterResult = runCatching { adapter.execute(plan) }.getOrElse {
            auditLog.append(
                AuditEvent(clock(), plan.sessionId, actionHash, AuditEventType.EXECUTION,
                    false, false, "Adapter execution failed")
            )
            auditLog.append(
                AuditEvent(clock(), plan.sessionId, actionHash, AuditEventType.VERIFICATION,
                    false, false, "Result verification failed")
            )
            return ExecutionResult(false, false, "Action execution failed")
        }

        auditLog.append(
            AuditEvent(clock(), plan.sessionId, actionHash, AuditEventType.EXECUTION,
                adapterResult.success, false,
                if (adapterResult.success) "Adapter execution succeeded" else "Adapter execution failed")
        )

        val verification = resultVerifier.verify(
            expectedSuccess = true,
            evidence = ActionResultEvidence(
                success = adapterResult.success,
                errorCode = if (adapterResult.success) null else "ADAPTER_EXECUTION_FAILED"
            )
        )
        auditLog.append(
            AuditEvent(clock(), plan.sessionId, actionHash, AuditEventType.VERIFICATION,
                adapterResult.success, verification.verified,
                if (verification.verified) "Result verification passed" else "Result verification failed")
        )

        return ExecutionResult(
            success = adapterResult.success && verification.verified,
            verified = verification.verified,
            message = verification.reason
        )
    }
}
