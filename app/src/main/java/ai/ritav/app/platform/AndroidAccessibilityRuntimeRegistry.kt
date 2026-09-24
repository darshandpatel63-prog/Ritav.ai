package ai.ritav.app.platform

import ai.ritav.app.core.security.EmergencyStopController

internal data class AndroidAccessibilityRuntimeBinding(
    val gate: AndroidAccessibilityContextGate,
    val bridge: AndroidAccessibilityModelContextBridge,
    val currentStopGeneration: () -> Long
)

internal object AndroidAccessibilityRuntimeRegistry {
    @Volatile
    private var binding: AndroidAccessibilityRuntimeBinding? = null

    @Synchronized
    fun install(binding: AndroidAccessibilityRuntimeBinding) {
        this.binding = binding
    }

    @Synchronized
    fun clear(gate: AndroidAccessibilityContextGate) {
        if (binding?.gate === gate) binding = null
    }

    fun current(): AndroidAccessibilityRuntimeBinding? = binding
}