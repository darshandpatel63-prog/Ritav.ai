package ai.ritav.app.core.security

/** Metadata describing what an app adapter is allowed to expose to Ritav. */
data class AppCapabilitySpec(
    val packageName: String,
    val capability: Capability,
    val actions: Set<String>,
    val riskTier: RiskTier,
    val sensitiveContentBlocked: Boolean = true,
    val financialCategory: Boolean = false,
    /** SHA-256 digest of the trusted Android signing certificate for this package. */
    val trustedCertificateSha256: String? = null
)

/**
 * Explicit registry for app capabilities.
 *
 * Unknown apps have no capabilities. Invalid or conflicting registrations are
 * rejected at construction time so adapter metadata cannot silently weaken the
 * deterministic security boundary.
 */
class AppCapabilityRegistry(
    specs: Collection<AppCapabilitySpec> = emptyList()
) {
    private val immutableSpecs = specs.map { it.copy(actions = it.actions.toSet()) }

    init {
        immutableSpecs.forEach { validate(it) }
        require(immutableSpecs.groupBy { Triple(it.packageName, it.capability, it.actions) }
            .values.all { entries -> entries.map { it.riskTier }.distinct().size <= 1 }) {
            "Conflicting risk metadata for the same app capability/action set"
        }
        require(immutableSpecs.none { it.financialCategory && it.capability != Capability.FINANCIAL_ACTION }) {
            "Financial category must use FINANCIAL_ACTION capability"
        }
        require(immutableSpecs.groupBy { it.packageName }.values.all { entries ->
            entries.map { it.trustedCertificateSha256 }.distinct().size <= 1
        }) {
            "Conflicting trusted certificate metadata for the same app package"
        }
    }

    private val specsByPackage = immutableSpecs.groupBy { it.packageName }

    fun isRegistered(packageName: String): Boolean =
        specsByPackage.containsKey(packageName)

    fun allows(packageName: String, capability: Capability, action: String, requestedRisk: RiskTier): Boolean =
        specsByPackage[packageName].orEmpty().any {
            it.capability == capability &&
                action in it.actions &&
                !it.financialCategory &&
                it.riskTier == requestedRisk &&
                it.sensitiveContentBlocked
        }

    fun isFinancial(packageName: String): Boolean =
        specsByPackage[packageName].orEmpty().any { it.financialCategory }

    fun riskTierFor(packageName: String, capability: Capability, action: String): RiskTier? =
        specsByPackage[packageName].orEmpty()
            .firstOrNull { capability == it.capability && action in it.actions }
            ?.riskTier

    fun trustedCertificateSha256(packageName: String): String? =
        specsByPackage[packageName].orEmpty().firstOrNull()?.trustedCertificateSha256

    fun specsFor(packageName: String): List<AppCapabilitySpec> =
        specsByPackage[packageName].orEmpty()

    private fun validate(spec: AppCapabilitySpec) {
        require(spec.packageName.length <= MAX_PACKAGE_NAME_LENGTH && spec.packageName.matches(PACKAGE_NAME_REGEX)) { "Invalid package name" }
        require(spec.actions.isNotEmpty()) { "Capability must expose at least one action" }
        require(spec.actions.all { it.isNotBlank() && it.length <= MAX_ACTION_LENGTH }) {
            "Invalid capability action"
        }
        spec.trustedCertificateSha256?.let {
            require(it.matches(CERTIFICATE_DIGEST_REGEX)) { "Invalid trusted certificate digest" }
        }
        if (spec.financialCategory) {
            require(spec.capability == Capability.FINANCIAL_ACTION)
            require(spec.riskTier == RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED)
        }
    }

    private companion object {
        val PACKAGE_NAME_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
        const val MAX_ACTION_LENGTH = 128
        const val MAX_PACKAGE_NAME_LENGTH = 256
        val CERTIFICATE_DIGEST_REGEX = Regex("^[A-Fa-f0-9]{64}$")
    }
}
