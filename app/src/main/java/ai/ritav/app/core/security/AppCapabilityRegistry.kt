package ai.ritav.app.core.security

/** Metadata describing what an app adapter is allowed to expose to Ritav. */
data class AppCapabilitySpec(
    val packageName: String,
    val capability: Capability,
    val actions: Set<String>,
    val riskTier: RiskTier,
    val sensitiveContentBlocked: Boolean = true,
    val financialCategory: Boolean = false
)

/**
 * Explicit registry for app capabilities.
 *
 * The registry is intentionally conservative: unknown apps have no registered
 * capabilities, and financial entries cannot be converted into ordinary grants.
 */
class AppCapabilityRegistry(
    specs: Collection<AppCapabilitySpec> = emptyList()
) {
    private val specsByPackage = specs.groupBy { it.packageName }

    fun isRegistered(packageName: String): Boolean =
        specsByPackage.containsKey(packageName)

    fun allows(packageName: String, capability: Capability, action: String): Boolean =
        specsByPackage[packageName].orEmpty().any {
            it.capability == capability && action in it.actions && !it.financialCategory
        }

    fun isFinancial(packageName: String): Boolean =
        specsByPackage[packageName].orEmpty().any { it.financialCategory }

    fun specsFor(packageName: String): List<AppCapabilitySpec> =
        specsByPackage[packageName].orEmpty()
}
