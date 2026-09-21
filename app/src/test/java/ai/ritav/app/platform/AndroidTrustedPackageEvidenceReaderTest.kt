package ai.ritav.app.platform

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTrustedPackageEvidenceReaderTest {
    private val certificate = "ritav-test-certificate".toByteArray()

    @Test
    fun exactSingleSignerProducesOnlyCertificateDigestEvidence() {
        val reader = AndroidTrustedPackageEvidenceReader(
            context = throw UnsupportedOperationException(),
            certificateReader = AndroidPackageSigningCertificateReader { listOf(certificate) }
        )

        val evidence = reader.read("com.example.safe")

        assertEquals("com.example.safe", evidence?.packageName)
        assertEquals(1, evidence?.signerCount)
        assertEquals(sha256(certificate), evidence?.certificateSha256)
    }

    @Test
    fun malformedPackageNameIsRejectedBeforeCertificateRead() {
        var reads = 0
        val reader = AndroidTrustedPackageEvidenceReader(
            context = throw UnsupportedOperationException(),
            certificateReader = AndroidPackageSigningCertificateReader {
                reads++
                listOf(certificate)
            }
        )

        assertNull(reader.read(""))
        assertNull(reader.read("not a package"))
        assertEquals(0, reads)
    }

    @Test
    fun missingOrUnreadableCertificateFailsClosed() {
        val reader = AndroidTrustedPackageEvidenceReader(
            context = throw UnsupportedOperationException(),
            certificateReader = AndroidPackageSigningCertificateReader { null }
        )

        assertNull(reader.read("com.example.safe"))
    }

    @Test
    fun multipleSignersFailClosed() {
        val reader = AndroidTrustedPackageEvidenceReader(
            context = throw UnsupportedOperationException(),
            certificateReader = AndroidPackageSigningCertificateReader {
                listOf(certificate, "second".toByteArray())
            }
        )

        assertNull(reader.read("com.example.safe"))
    }

    @Test
    fun certificateBytesNeverAppearInEvidence() {
        val reader = AndroidTrustedPackageEvidenceReader(
            context = throw UnsupportedOperationException(),
            certificateReader = AndroidPackageSigningCertificateReader { listOf(certificate) }
        )

        val evidence = requireNotNull(reader.read("com.example.safe"))
        assertFalse(evidence.certificateSha256.contains("ritav-test-certificate"))
        assertTrue(evidence.certificateSha256.matches(Regex("^[a-f0-9]{64}$")))
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
}
