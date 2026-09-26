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

/** Sanitized capability metadata suitable for user-facing authorization flows. */
data class CapabilityGrantCandidate(
    val packageName: String,
    val capability: Capability,
    val action: String,
    val riskTier: RiskTier
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
    private var specsSnapshot = specs.map { it.copy(actions = it.actions.toSet()) }

    init {
        validateAll(specsSnapshot)
    }

    @Synchronized
    fun isRegistered(packageName: String): Boolean =
        specsSnapshot.any { it.packageName == packageName }

    @Synchronized
    fun allows(
        packageName: String,
        capability: Capability,
        action: String,
        requestedRisk: RiskTier
    ): Boolean {
        if (capability == Capability.FINANCIAL_ACTION) return false
        val matches = matchingSpecs(packageName, capability, action)
        if (matches.isEmpty() || matches.map { it.riskTier }.distinct().size != 1) return false
        return matches.any {
            !it.financialCategory &&
                it.riskTier == requestedRisk &&
                it.sensitiveContentBlocked
        }
    }

    @Synchronized
    fun isFinancial(packageName: String): Boolean =
        specsSnapshot.filter { it.packageName == packageName }.any { it.financialCategory }

    @Synchronized
    fun riskTierFor(
        packageName: String,
        capability: Capability,
        action: String
    ): RiskTier? {
        if (capability == Capability.FINANCIAL_ACTION) return null
        val matches = matchingSpecs(packageName, capability, action)
        return matches.map { it.riskTier }.distinct().singleOrNull()
    }

    /**
     * Returns only user-grantable actions for the current Android composition.
     *
     * Certificate material is intentionally not exposed to the UI layer. A
     * certificate pin is required so a displayed app action is also executable
     * by the Android trusted-package adapter.
     */
    @Synchronized
    internal fun capabilityGrantCandidates(): List<CapabilityGrantCandidate> =
        specsSnapshot.asSequence()
            .filter {
                !it.financialCategory &&
                    it.capability != Capability.FINANCIAL_ACTION &&
                    it.riskTier != RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED &&
                    it.sensitiveContentBlocked &&
                    it.trustedCertificateSha256 != null
            }
            .flatMap { spec ->
                spec.actions.asSequence().map { action ->
                    CapabilityGrantCandidate(
                        packageName = spec.packageName,
                        capability = spec.capability,
                        action = action,
                        riskTier = spec.riskTier
                    )
                }
            }
            .sortedWith(
                compareBy(
                    CapabilityGrantCandidate::packageName,
                    { it.capability.name },
                    CapabilityGrantCandidate::action
                )
            )
            .toList()

    @Synchronized
    fun trustedCertificateSha256(packageName: String): String? =
        specsSnapshot.firstOrNull { it.packageName == packageName }?.trustedCertificateSha256

    @Synchronized
    fun specsFor(packageName: String): List<AppCapabilitySpec> =
        specsSnapshot.filter { it.packageName == packageName }.map { it.copy(actions = it.actions.toSet()) }

    /**
     * Security-internal mutation used only after a separate reviewed trust-entry
     * decision has been authorized and durably persisted.
     */
    @Synchronized
    internal fun addReviewedTrustedAppSpec(spec: AppCapabilitySpec): Boolean {
        if (!isValidReviewedTrustedAppSpec(spec)) return false
        if (specsSnapshot.any { it.packageName == spec.packageName }) return false

        val next = specsSnapshot + spec.copy(actions = spec.actions.toSet())
        return runCatching {
            validateAll(next)
            specsSnapshot = next
            true
        }.getOrDefault(false)
    }

    /** Security-internal removal/rollback for a reviewed trusted-app spec. */
    @Synchronized
    internal fun removeReviewedTrustedAppSpec(spec: AppCapabilitySpec): Boolean {
        val index = specsSnapshot.indexOfFirst { it == spec }
        if (index < 0) return false
        specsSnapshot = specsSnapshot.toMutableList().also { it.removeAt(index) }
        return true
    }

    private fun matchingSpecs(
        packageName: String,
        capability: Capability,
        action: String
    ): List<AppCapabilitySpec> =
        specsSnapshot.filter {
            it.packageName == packageName &&
                it.capability == capability &&
                action in it.actions
        }

    private fun validateAll(entries: Collection<AppCapabilitySpec>) {
        entries.forEach { validate(it) }
        require(entries.groupBy { Triple(it.packageName, it.capability, it.actions) }
            .values.all { group -> group.map { it.riskTier }.distinct().size <= 1 }) {
            "Conflicting risk metadata for the same app capability/action set"
        }
        require(entries.none { it.financialCategory && it.capability != Capability.FINANCIAL_ACTION }) {
            "Financial category must use FINANCIAL_ACTION capability"
        }
        require(entries.groupBy { it.packageName }.values.all { group ->
            group.map { it.trustedCertificateSha256 }.distinct().size <= 1
        }) {
            "Conflicting trusted certificate metadata for the same app package"
        }
    }

    private fun validate(spec: AppCapabilitySpec) {
        require(
            spec.packageName.length <= MAX_PACKAGE_NAME_LENGTH &&
                spec.packageName.matches(PACKAGE_NAME_REGEX)
        ) { "Invalid package name" }
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
        const val MAX_ACTION_LENGTH = 128
        const val MAX_PACKAGE_NAME_LENGTH = 256
        val PACKAGE_NAME_REGEX = Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$""")
        val CERTIFICATE_DIGEST_REGEX = Regex("^[A-Fa-f0-9]{64}$")
    }
}

/**
 * Current production trust-entry scope.
 *
 * The concrete Android adapter only implements APP_LAUNCH + "open", so a
 * provisioning write is deliberately constrained to that exact capability.
 * Broader capabilities require their own reviewed adapter/policy scope.
 */
internal fun isValidReviewedTrustedAppSpec(spec: AppCapabilitySpec): Boolean =
    spec.capability == Capability.APP_LAUNCH &&
        spec.actions == setOf("open") &&
        spec.riskTier == RiskTier.TIER_1_REVERSIBLE &&
        spec.sensitiveContentBlocked &&
        !spec.financialCategory &&
        spec.trustedCertificateSha256?.matches(Regex("^[A-Fa-f0-9]{64}$")) == true &&
        spec.packageName.length in 1..256 &&
        spec.packageName.matches(Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$"""))
