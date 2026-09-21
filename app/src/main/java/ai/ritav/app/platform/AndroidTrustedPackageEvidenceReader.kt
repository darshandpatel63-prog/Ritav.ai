package ai.ritav.app.platform

import android.content.Context
import java.security.MessageDigest

/**
 * Read-only evidence about the currently installed Android package identity.
 *
 * This is provisioning evidence, not trust. It never changes the registry,
 * grants capabilities, or authorizes execution. Callers must treat package
 * metadata as untrusted until a separate reviewed trust decision is made.
 */
internal data class AndroidTrustedPackageEvidence(
    val packageName: String,
    val certificateSha256: String,
    val signerCount: Int
)

/**
 * Produces bounded signing-certificate evidence for a specific installed package.
 *
 * A single signer is required for an evidence record. Missing/unreadable package
 * metadata, malformed input, and multi-signer identities fail closed.
 */
internal class AndroidTrustedPackageEvidenceReader(
    context: Context,
    private val certificateReader: AndroidPackageSigningCertificateReader =
        ContextAndroidPackageSigningCertificateReader(context.applicationContext)
) {
    fun read(packageName: String): AndroidTrustedPackageEvidence? {
        if (!isBoundedPackageName(packageName)) return null

        val certificates = runCatching {
            certificateReader.read(packageName)
        }.getOrNull() ?: return null

        if (certificates.size != 1) return null

        return AndroidTrustedPackageEvidence(
            packageName = packageName,
            certificateSha256 = certificates.single().sha256Hex(),
            signerCount = certificates.size
        )
    }

    private fun isBoundedPackageName(packageName: String): Boolean =
        packageName.length in 1..256 &&
            packageName.matches(PACKAGE_NAME_REGEX)

    private fun ByteArray.sha256Hex(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }

    private companion object {
        val PACKAGE_NAME_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
    }
}
