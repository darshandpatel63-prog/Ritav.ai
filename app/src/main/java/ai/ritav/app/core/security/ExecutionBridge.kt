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
    private val emergencyStop: EmergencyStopController = securityPipeline.emergencyStopController(),
    private val taskRuntimeState: TaskRuntimeStateStore = TaskRuntimeStateStore(clock)
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
            taskRuntimeState.update(
                state = TaskRuntimeState.BLOCKED,
                taskId = plan.stableHash(),
                taskName = "\${plan.capability.name}: \${plan.action}",
                currentStep = "Security validation",
                summary = "Execution could not start because the security clock was unavailable."
            )
            return ExecutionResult(false, false, "Security clock unavailable")
        }
        if (now < 0L) {
            taskRuntimeState.update(
                state = TaskRuntimeState.BLOCKED,
                taskId = plan.stableHash(),
                taskName = "\${plan.capability.name}: \${plan.action}",
                currentStep = "Security validation",
                summary = "Execution could not start because the security clock was unavailable."
            )
            return ExecutionResult(false, false, "Security clock unavailable")
        }
        val taskId = plan.stableHash()
        val taskName = "\${plan.capability.name}: \${plan.action}"
        if (!plan.isValid()) {
            taskRuntimeState.update(
                state = TaskRuntimeState.BLOCKED,
                taskId = taskId,
                taskName = taskName,
                currentStep = "Security validation",
                summary = "The action plan failed deterministic validation."
            )
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
            taskRuntimeState.update(
                state = TaskRuntimeState.BLOCKED,
                taskId = taskId,
                taskName = taskName,
                currentStep = "Capability policy",
                summary = "Execution was blocked by capability policy."
            )
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
            taskRuntimeState.update(
                state = if (emergencyStop.isActive()) {
                    TaskRuntimeState.STOPPED
                } else {
                    TaskRuntimeState.BLOCKED
                },
                taskId = taskId,
                taskName = taskName,
                currentStep = "Security authorization",
                summary = if (emergencyStop.isActive()) {
                    "Emergency Stop is active; protected execution is unavailable."
                } else {
                    "Execution was blocked by deterministic security authorization."
                }
            )
            auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
                false, false, securityDecision.reason))
            return ExecutionResult(false, false, securityDecision.reason)
        }

        auditLog.append(AuditEvent(safeClock(now), plan.sessionId, actionHash, AuditEventType.POLICY_DECISION,
            true, false, "Capability and security pipeline checks passed"))

        taskRuntimeState.update(
            state = TaskRuntimeState.EXECUTING,
            taskId = taskId,
            taskName = taskName,
            currentStep = "Dispatching approved action",
            summary = "Approved action is being dispatched through the security-owned adapter."
        )

        val adapterAttempt = emergencyStop.runIfInactive {
            runCatching { adapter.execute(plan) }
        }

        if (adapterAttempt == null) {
            taskRuntimeState.update(
                state = TaskRuntimeState.STOPPED,
                taskId = taskId,
                taskName = taskName,
                currentStep = "Dispatch",
                summary = "Emergency Stop became active before adapter dispatch."
            )
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
            taskRuntimeState.update(
                state = TaskRuntimeState.FAILED_SAFELY,
                taskId = taskId,
                taskName = taskName,
                currentStep = "Dispatch",
                summary = "The execution adapter failed; success was not reported."
            )
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
            taskRuntimeState.update(
                state = TaskRuntimeState.FAILED_SAFELY,
                taskId = taskId,
                taskName = taskName,
                currentStep = "Verification",
                summary = "The action did not complete successfully; verification was not reported as successful."
            )
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

        taskRuntimeState.update(
            state = TaskRuntimeState.VERIFYING,
            taskId = taskId,
            taskName = taskName,
            currentStep = "Result verification",
            summary = "The execution result is being checked against the expected state."
        )

        val verificationCheckedAtMillis = runCatching { clock() }
            .getOrNull()
            ?.takeIf { it >= 0L }

        if (verificationCheckedAtMillis == null) {
            taskRuntimeState.update(
                state = TaskRuntimeState.FAILED_SAFELY,
                taskId = taskId,
                taskName = taskName,
                currentStep = "Result verification",
                summary = "Verification could not complete because the security clock was unavailable."
            )
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
        taskRuntimeState.update(
            state = if (verification.verified) {
                TaskRuntimeState.COMPLETED
            } else {
                TaskRuntimeState.FAILED_SAFELY
            },
            taskId = taskId,
            taskName = taskName,
            currentStep = "Result verification",
            summary = if (verification.verified) {
                "Observed result state matched the expected state."
            } else {
                "Observed result state did not satisfy deterministic verification."
            }
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
