package ai.ritav.app.core.security

import java.lang.reflect.InvocationTargetException

/**
 * Test-only construction/legacy authorization probes. Production code does not
 * expose the raw token gate or caller-controlled token minting surface.
 */
fun testAuthorizationService(
    emergencyStop: EmergencyStopController = EmergencyStopController(),
    clockEpochMillis: () -> Long = System::currentTimeMillis
): ActionAuthorizationService = ActionAuthorizationService(
    deviceAuthorization = StubDeviceAuthorizationGateway(available = true, result = true),
    clockEpochMillis = clockEpochMillis,
    emergencyStop = emergencyStop
)

/**
 * Test-only raw issuance probe for low-level token-boundary regression coverage.
 * This uses reflection solely because the production raw issuer is private.
 */
fun ActionAuthorizationService.issue(
    plan: ActionPlan,
    requiredLevel: AuthorizationLevel,
    nowEpochMillis: Long,
    ttlMillis: Long = 60_000L
): String {
    val gate = privateTokenGate()
    val method = gate.javaClass.getDeclaredMethod(
        "issue",
        ActionPlan::class.java,
        AuthorizationLevel::class.java,
        Long::class.javaPrimitiveType,
        Long::class.javaPrimitiveType
    ).apply { isAccessible = true }

    return invokeUnwrapped {
        method.invoke(gate, plan, requiredLevel, nowEpochMillis, ttlMillis)
    } as String
}

/** Test-only deterministic consume probe for caller-time regression tests. */
fun ActionAuthorizationService.consume(
    token: String,
    plan: ActionPlan,
    providedLevel: AuthorizationLevel,
    nowEpochMillis: Long
): Boolean {
    val gate = privateTokenGate()
    val method = gate.javaClass.getDeclaredMethod(
        "consumeAt",
        String::class.java,
        ActionPlan::class.java,
        AuthorizationLevel::class.java,
        Long::class.javaPrimitiveType
    ).apply { isAccessible = true }

    return invokeUnwrapped {
        method.invoke(gate, token, plan, providedLevel, nowEpochMillis)
    } as Boolean
}

private fun ActionAuthorizationService.privateTokenGate(): Any =
    javaClass.getDeclaredField("tokenGate").apply { isAccessible = true }.get(this)!!

private fun invokeUnwrapped(block: () -> Any?): Any? =
    try {
        block()
    } catch (error: InvocationTargetException) {
        throw (error.targetException ?: error)
    }
