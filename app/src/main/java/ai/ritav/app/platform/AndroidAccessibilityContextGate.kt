package ai.ritav.app.platform

import ai.ritav.app.core.orchestrator.AgentCapabilityScope
import ai.ritav.app.core.orchestrator.ModelBackedSpecialistAgent
import ai.ritav.app.core.orchestrator.ScreenContextSecurityFilter
import ai.ritav.app.core.orchestrator.ScreenContextSnapshot
import ai.ritav.app.core.orchestrator.ScreenContentSource
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.SecuritySession
import ai.ritav.app.core.security.UntrustedContent

/**
 * In-memory task-scoped accessibility access contract.
 *
 * AccessibilityService may be enabled by the operating system, but this gate
 * keeps content collection disabled unless an explicit bounded task request is
 * armed. A stop-generation change invalidates the request.
 */
internal class AndroidAccessibilityContextGate(
    private val filter: ScreenContextSecurityFilter = ScreenContextSecurityFilter()
) {
    private data class ArmedRequest(
        val taskId: String,
        val packageName: String,
        val agentId: String,
        val allowedCapabilities: Set<Capability>,
        val expiresAtElapsedRealtime: Long,
        val stopGeneration: Long
    )

    @Volatile
    private var armed: ArmedRequest? = null

    @Synchronized
    fun arm(
        taskId: String,
        packageName: String,
        agentId: String,
        scope: AgentCapabilityScope,
        expiresAtElapsedRealtime: Long,
        nowElapsedRealtime: Long,
        stopGeneration: Long
    ): Boolean {
        if (taskId.isBlank() || taskId.length > MAX_TASK_ID_LENGTH) return false
        if (!isValidPackageName(packageName)) return false
        if (agentId.isBlank() || agentId.length > MAX_AGENT_ID_LENGTH) return false
        if (Capability.READ_ALLOWED_CONTENT !in scope.allowedCapabilities) return false
        if (nowElapsedRealtime < 0L || expiresAtElapsedRealtime <= nowElapsedRealtime) return false
        if (expiresAtElapsedRealtime - nowElapsedRealtime > MAX_SESSION_MILLIS) return false
        if (stopGeneration < 0L) return false

        armed = ArmedRequest(
            taskId = taskId,
            packageName = packageName,
            agentId = agentId,
            allowedCapabilities = scope.allowedCapabilities.toSet(),
            expiresAtElapsedRealtime = expiresAtElapsedRealtime,
            stopGeneration = stopGeneration
        )
        return true
    }

    @Synchronized
    fun disarm() {
        armed = null
    }

    /**
     * Filters before publication. Raw screen text is never retained by this
     * gate after the method returns.
     */
    @Synchronized
    fun prepare(
        snapshot: ScreenContextSnapshot,
        nowElapsedRealtime: Long,
        currentStopGeneration: Long
    ): PreparedAccessibilityContext? {
        val active = armed ?: return null
        if (nowElapsedRealtime < 0L || currentStopGeneration < 0L) return null
        if (nowElapsedRealtime >= active.expiresAtElapsedRealtime) {
            armed = null
            return null
        }
        if (active.stopGeneration != currentStopGeneration) {
            armed = null
            return null
        }
        if (snapshot.taskId != active.taskId || snapshot.packageName != active.packageName) return null
        if (Capability.READ_ALLOWED_CONTENT !in active.allowedCapabilities) return null

        val filtered = filter.filter(snapshot) ?: return null

        return PreparedAccessibilityContext(
            taskId = active.taskId,
            agentId = active.agentId,
            allowedCapabilities = active.allowedCapabilities,
            context = UntrustedContent(
                text = filtered.text,
                source = filtered.source.name.lowercase() + ":" + filtered.packageName,
                trustLevel = ContentTrustLevel.APP_CONTENT
            )
        )
    }

    private fun isValidPackageName(packageName: String): Boolean =
        packageName.length in 1..MAX_PACKAGE_NAME_LENGTH &&
            packageName.matches(PACKAGE_NAME_REGEX)

    internal data class PreparedAccessibilityContext(
        val taskId: String,
        val agentId: String,
        val allowedCapabilities: Set<Capability>,
        val context: UntrustedContent
    )

    private companion object {
        const val MAX_TASK_ID_LENGTH = 256
        const val MAX_PACKAGE_NAME_LENGTH = 256
        const val MAX_AGENT_ID_LENGTH = 256
        const val MAX_SESSION_MILLIS = 30_000L
        val PACKAGE_NAME_REGEX = Regex("""^[A-Za-z][A-Za-z0-9_]*(.[A-Za-z0-9_]+)+$""")
    }
}

/**
 * Narrow producer-to-model bridge for accessibility content.
 *
 * The bridge consumes only the filtered APP_CONTENT envelope and then enters
 * the existing SecureModelRuntimeGateway through ModelBackedSpecialistAgent.
 */
internal class AndroidAccessibilityModelContextBridge(
    private val modelAgent: ModelBackedSpecialistAgent,
    private val gate: AndroidAccessibilityContextGate
) {
    fun consume(
        snapshot: ScreenContextSnapshot,
        nowElapsedRealtime: Long,
        currentStopGeneration: Long
    ): ai.ritav.app.core.orchestrator.AgentProposal? {
        val prepared = gate.prepare(snapshot, nowElapsedRealtime, currentStopGeneration)
            ?: return null

        val request = ai.ritav.app.core.orchestrator.AgentRequest.createFromContext(
            taskId = prepared.taskId,
            userCommand = UntrustedContent(
                text = "Use the approved user task only",
                source = "accessibility-bridge",
                trustLevel = ContentTrustLevel.USER_COMMAND
            ),
            context = listOf(prepared.context),
            scope = AgentCapabilityScope(prepared.allowedCapabilities)
        ) ?: return null

        return modelAgent.propose(request)?.takeIf { it.agentId == prepared.agentId }
    }
}
