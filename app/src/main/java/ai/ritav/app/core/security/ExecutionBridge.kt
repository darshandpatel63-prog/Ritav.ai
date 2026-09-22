package ai.ritav.app.core.security

/** Only approved Android adapters implement this interface. */
interface AndroidActionAdapter {
    fun execute(plan: ActionPlan): ExecutionResult
}

data class ExecutionResult(
    val success: Boolean,
    /** True only when the adapter has completed its required deterministic post-action verification. */
    val verified: Boolean,
    val message: String,
    val observedState: String? = null
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
        val now = runCatching { clock() }.getOrElse {
            return ExecutionResult(false, false, "Security clock unavailable")
        }
        if (now < 0L) {
            return ExecutionResult(false, false, "Security clock unavailable")
        }
        if (!plan.isValid()) {
            auditLog.append(AuditEvent(safeClock(now), plan.sessionId, null, AuditEventType.POLICY_DECISION,
                false, false, "Action plan is malformed or exceeds security bounds"))
            return ExecutionResult(false, false, "Action plan is malformed or exceeds security bounds")
        }
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
            auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
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
            auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
                false, false, securityDecision.reason))
            return ExecutionResult(false, false, securityDecision.reason)
        }

        auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
            true, false, "Capability and security pipeline checks passed"))

        val adapterResult = runCatching { adapter.execute(plan) }.getOrElse {
            auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.EXECUTION,
                false, false, "Adapter execution failed"))
            auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.VERIFICATION,
                false, false, "Result verification failed"))
            return ExecutionResult(false, false, "Action execution failed")
        }

        auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.EXECUTION,
            adapterResult.success, false,
            if (adapterResult.success) "Adapter execution succeeded" else "Adapter execution failed"))

        if (!adapterResult.success) {
            val verification = resultVerifier.verify(
                expectedSuccess = false,
                evidence = ActionResultEvidence(
                    success = adapterResult.success,
                    observedState = adapterResult.observedState,
                    errorCode = "ADAPTER_EXECUTION_FAILED"
                ),
                expectedState = plan.expectedState
            )
            auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.VERIFICATION,
                false, verification.verified, "Result verification failed"))
            return ExecutionResult(false, false, verification.reason, adapterResult.observedState)
        }

        if (!adapterResult.verified) {
            auditLog.append(AuditEvent(
                safeClock(now),
                plan.sessionId,
                actionHash,
                AuditEventType.VERIFICATION,
                true,
                false,
                "Adapter execution completed without required independent result verification"
            ))
            return ExecutionResult(
                success = false,
                verified = false,
                message = "Required independent result verification was not completed",
                observedState = adapterResult.observedState
            )
        }

        val verification = resultVerifier.verify(
            expectedSuccess = true,
            evidence = ActionResultEvidence(
                success = true,
                observedState = adapterResult.observedState
            ),
            expectedState = plan.expectedState
        )
        auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.VERIFICATION,
            true, verification.verified,
            if (verification.verified) "Result verification passed" else "Result verification failed"))

        return ExecutionResult(
            success = verification.verified,
            verified = verification.verified,
            message = verification.reason,
            observedState = adapterResult.observedState
        )
    }

    private fun safeClock(fallback: Long): Long = runCatching { clock() }
        .getOrDefault(fallback)
        .takeIf { it >= 0L }
        ?: fallback
}
