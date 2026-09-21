package ai.ritav.app.core.security

import ai.ritav.app.core.storage.SecureLocalStore
import java.nio.charset.StandardCharsets

internal sealed interface TrustedAppEntrySnapshot {
    object Unconfigured : TrustedAppEntrySnapshot
    data class Loaded(val specs: List<AppCapabilitySpec>) : TrustedAppEntrySnapshot
    object Invalid : TrustedAppEntrySnapshot
}

/**
 * Durable store for reviewed trusted-app metadata.
 *
 * Only the narrow current Android trust-entry shape (APP_LAUNCH + "open") is
 * persisted. Invalid/corrupted stored state is rejected rather than repaired
 * or partially trusted.
 */
internal interface TrustedAppEntryStore {
    fun snapshot(): TrustedAppEntrySnapshot
    fun add(spec: AppCapabilitySpec): Boolean
    fun remove(spec: AppCapabilitySpec): Boolean
}

internal class SecureTrustedAppEntryStore(
    private val readRaw: () -> String?,
    private val writeRaw: (String) -> Unit,
    private val removeRaw: () -> Unit
) : TrustedAppEntryStore {

    internal constructor(store: SecureLocalStore) : this(
        readRaw = { store.getString(STORAGE_KEY) },
        writeRaw = { value -> store.putString(STORAGE_KEY, value) },
        removeRaw = { store.remove(STORAGE_KEY) }
    )

    @Synchronized
    override fun snapshot(): TrustedAppEntrySnapshot {
        val raw = runCatching { readRaw() }.getOrElse { return TrustedAppEntrySnapshot.Invalid }
            ?: return TrustedAppEntrySnapshot.Unconfigured

        if (raw.isBlank()) return TrustedAppEntrySnapshot.Unconfigured
        if (raw.toByteArray(StandardCharsets.UTF_8).size > MAX_SERIALIZED_BYTES) {
            return TrustedAppEntrySnapshot.Invalid
        }

        return runCatching {
            val lines = raw.lines()
            require(lines.size in 1..MAX_ENTRIES)
            val specs = lines.map(::decode)
            require(specs.distinct().size == specs.size)
            require(specs.all(::isValidReviewedTrustedAppSpec))
            AppCapabilityRegistry(specs)
            TrustedAppEntrySnapshot.Loaded(
                specs.sortedWith(compareBy(AppCapabilitySpec::packageName))
            )
        }.getOrElse { TrustedAppEntrySnapshot.Invalid }
    }

    @Synchronized
    override fun add(spec: AppCapabilitySpec): Boolean {
        if (!isValidReviewedTrustedAppSpec(spec)) return false

        val existing = when (val snapshot = snapshot()) {
            TrustedAppEntrySnapshot.Unconfigured -> emptyList()
            is TrustedAppEntrySnapshot.Loaded -> snapshot.specs
            TrustedAppEntrySnapshot.Invalid -> return false
        }

        if (existing.any { it.packageName == spec.packageName }) return false
        val next = (existing + spec.copy(actions = spec.actions.toSet()))
            .sortedWith(compareBy(AppCapabilitySpec::packageName))
        if (next.size > MAX_ENTRIES) return false

        return runCatching {
            AppCapabilityRegistry(next)
            writeRaw(serialize(next))
            true
        }.getOrDefault(false)
    }

    @Synchronized
    override fun remove(spec: AppCapabilitySpec): Boolean {
        val existing = when (val snapshot = snapshot()) {
            TrustedAppEntrySnapshot.Unconfigured -> return false
            is TrustedAppEntrySnapshot.Loaded -> snapshot.specs
            TrustedAppEntrySnapshot.Invalid -> return false
        }
        if (spec !in existing) return false

        val next = existing.filterNot { it == spec }
        return runCatching {
            if (next.isEmpty()) removeRaw() else writeRaw(serialize(next))
            true
        }.getOrDefault(false)
    }

    private fun serialize(specs: List<AppCapabilitySpec>): String =
        specs.joinToString("\n") { spec ->
            listOf(
                spec.packageName,
                spec.capability.name,
                spec.actions.single(),
                spec.riskTier.name,
                spec.trustedCertificateSha256.orEmpty()
            ).joinToString("|")
        }.also {
            require(it.toByteArray(StandardCharsets.UTF_8).size <= MAX_SERIALIZED_BYTES)
        }

    private fun decode(line: String): AppCapabilitySpec {
        val parts = line.split('|')
        require(parts.size == 5)

        val spec = AppCapabilitySpec(
            packageName = parts[0],
            capability = Capability.valueOf(parts[1]),
            actions = setOf(parts[2]),
            riskTier = RiskTier.valueOf(parts[3]),
            sensitiveContentBlocked = true,
            financialCategory = false,
            trustedCertificateSha256 = parts[4].takeIf { it.isNotEmpty() }
        )
        require(isValidReviewedTrustedAppSpec(spec))
        return spec
    }

    private companion object {
        const val STORAGE_KEY = "trusted_app_entries_v1"
        const val MAX_ENTRIES = 64
        const val MAX_SERIALIZED_BYTES = 96 * 1024
    }
}
