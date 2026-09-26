package ai.ritav.app.core.security

/** Internal adapter contract; final dispatch implementations are not a public app API. */
internal interface AndroidActionAdapter {
    fun execute(plan: ActionPlan): ExecutionResult
}

data class ExecutionResult(
    val success: Boolean,
    /** True only when the adapter has completed its required deterministic post-action verification. */
    val verified: Boolean,
    val message: String,
    val observedState: String? = null,
    /** Structured evidence consumed by the central semantic verifier. */
    val verificationEvidence: VerificationEvidence? = null
)

/** Public-safe execution port exposed by the trusted runtime composition root. */
interface SecureExecutionPort {
    fun execute(
        plan: ActionPlan,
        userExplicitlyRequested: Boolean,
        authorizationLevel: AuthorizationLevel = AuthorizationLevel.NONE,
        containsSensitiveData: Boolean = false,
        authorizationToken: String? = null,
        identitySession: SecuritySession? = null,
        inputText: String? = null
    ): ExecutionResult
}

/** Final execution implementation. Construction is internal to the app security module. */
internal class ExecutionBridge(
    private val capabilityPolicyGate: CapabilityPolicyGate,
    private val securityPipeline: SecurityExecutionPipeline,
    private val adapter: AndroidActionAdapter,
    private val semanticResultVerifier: SemanticResultVerifier = SemanticResultVerifier(),
    private val auditLog: AuditLog = securityPipeline.auditLog,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val emergencyStop: EmergencyStopController = securityPipeline.emergencyStopController()
) : SecureExecutionPort {
    override fun execute(
        plan: ActionPlan,
        userExplicitlyRequested: Boolean,
        authorizationLevel: AuthorizationLevel,
        containsSensitiveData: Boolean,
        authorizationToken: String?,
        identitySession: SecuritySession?,
        inputText: String?
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

        val adapterAttempt = emergencyStop.runIfInactive {
            runCatching { adapter.execute(plan) }
        }

        if (adapterAttempt == null) {
            auditLog.append(AuditEvent(
                safeClock(now), plan.sessionId, actionHash, AuditEventType.EXECUTION,
                false, false, "Emergency Stop became active before adapter dispatch"
            ))
            auditLog.append(AuditEvent(
                safeClock(now), plan.sessionId, actionHash, AuditEventType.VERIFICATION,
                false, false, "Result verification skipped because Emergency Stop blocked dispatch"
            ))
            return ExecutionResult(false, false, "Emergency Stop became active before adapter dispatch")
        }

        val adapterResult = adapterAttempt.getOrElse {
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
            auditLog.append(AuditEvent(
                safeClock(now),
                plan.sessionId,
                actionHash,
                AuditEventType.VERIFICATION,
                false,
                false,
                "Result verification skipped because adapter execution failed"
            ))
            return ExecutionResult(
                success = false,
                verified = false,
                message = "Action execution failed",
                observedState = adapterResult.observedState,
                verificationEvidence = adapterResult.verificationEvidence
            )
        }

        val verificationCheckedAtMillis = runCatching { clock() }
            .getOrNull()
            ?.takeIf { it >= 0L }

        if (verificationCheckedAtMillis == null) {
            auditLog.append(AuditEvent(
                safeClock(now),
                plan.sessionId,
                actionHash,
                AuditEventType.VERIFICATION,
                true,
                false,
                "Verification clock unavailable"
            ))
            return ExecutionResult(
                success = false,
                verified = false,
                message = "Verification clock unavailable",
                observedState = adapterResult.observedState,
                verificationEvidence = adapterResult.verificationEvidence
            )
        }

        val verification = semanticResultVerifier.verify(
            plan = plan,
            executionStartedAtMillis = now,
            verificationCheckedAtMillis = verificationCheckedAtMillis,
            executionSucceeded = adapterResult.success,
            observedState = adapterResult.observedState,
            evidence = adapterResult.verificationEvidence
        )
        auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.VERIFICATION,
            true, verification.verified,
            if (verification.verified) "Result verification passed" else "Result verification failed"))

        return ExecutionResult(
            success = verification.verified,
            verified = verification.verified,
            message = verification.reason,
            observedState = adapterResult.observedState,
            verificationEvidence = adapterResult.verificationEvidence
        )
    }

    private fun safeClock(fallback: Long): Long = runCatching { clock() }
        .getOrDefault(fallback)
        .takeIf { it >= 0L }
        ?: fallback
}
