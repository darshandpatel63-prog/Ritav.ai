package ai.ritav.app.core.storage

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal const val MAX_SECURE_STORE_VALUE_BYTES = 131_072
internal const val MAX_SECURE_STORE_NAME_LENGTH = 128
internal const val MAX_SECURE_STORE_ENCODED_LENGTH = 174_800

/**
 * Small platform-only encrypted store for security-sensitive local state.
 *
 * - AES-256-GCM for authenticated encryption.
 * - Key material stays in Android Keystore.
 * - No network or third-party storage dependency.
 * - Values are encrypted before entering SharedPreferences.
 * - Stored values are explicitly size-bounded to prevent unbounded resource use.
 *
 * This class is intentionally simple. Larger structured data should move to an
 * encrypted database layer only after that dependency is reviewed.
 */
class SecureLocalStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    @Synchronized
    fun putString(name: String, value: String) {
        validateSecureLocalStoreName(name)
        val plainText = value.toByteArray(StandardCharsets.UTF_8)
        validateSecureLocalStoreValueSize(plainText.size)
        val encrypted = encrypt(plainText)
        check(preferences.edit().putString(name, encrypted).commit()) {
            "Secure local write failed"
        }
    }

    @Synchronized
    fun getString(name: String): String? {
        validateSecureLocalStoreName(name)
        val encoded = preferences.getString(name, null) ?: return null
        validateSecureLocalStoreEncodedSize(encoded.length)
        val decrypted = decrypt(encoded)
        validateSecureLocalStoreValueSize(decrypted.size)
        return String(decrypted, StandardCharsets.UTF_8)
    }

    @Synchronized
    fun remove(name: String) {
        validateSecureLocalStoreName(name)
        check(preferences.edit().remove(name).commit()) {
            "Secure local delete failed"
        }
    }

    @Synchronized
    fun clear() {
        check(preferences.edit().clear().commit()) {
            "Secure local clear failed"
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE)
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encrypt(plainText: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText)
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): ByteArray {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size >= GCM_IV_LENGTH_BYTES + GCM_TAG_LENGTH_BYTES) {
            "Corrupt secure value"
        }
        require(combined.size <= MAX_SECURE_STORE_CIPHERTEXT_BYTES) {
            "Secure local value is too large"
        }
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val cipherText = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )
        return cipher.doFinal(cipherText)
    }

    private companion object {
        private const val PREFS_NAME = "ritav_secure_state"
        private const val KEY_ALIAS = "ritav_secure_state_aes"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8
        private const val MAX_SECURE_STORE_CIPHERTEXT_BYTES =
            MAX_SECURE_STORE_VALUE_BYTES + GCM_IV_LENGTH_BYTES + GCM_TAG_LENGTH_BYTES
    }
}

internal fun validateSecureLocalStoreName(name: String) {
    require(name.isNotBlank()) { "Preference key must not be blank" }
    require(name.length <= MAX_SECURE_STORE_NAME_LENGTH) { "Preference key is too long" }
}

internal fun validateSecureLocalStoreValueSize(utf8ByteCount: Int) {
    require(utf8ByteCount in 0..MAX_SECURE_STORE_VALUE_BYTES) {
        "Secure local value is too large"
    }
}

internal fun validateSecureLocalStoreEncodedSize(encodedCharCount: Int) {
    require(encodedCharCount in 1..MAX_SECURE_STORE_ENCODED_LENGTH) {
        "Secure local encoded value is too large"
    }
}
