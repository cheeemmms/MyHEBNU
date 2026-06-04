package com.myhebnu.data.remote

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * RSA encryption utility for CAS SSO password encryption.
 *
 * The教务系统 CAS server exposes a public key via `/cas/v2/getPubKey`,
 * which is used to encrypt the password before submitting the login form.
 */
object CryptoUtil {

    private const val CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding"

    /**
     * Parse a raw modulus + exponent JSON response into a [PublicKey].
     *
     * The CAS server returns: `{ "modulus": "...", "exponent": "..." }`
     * Both values are hex-encoded.
     */
    fun parsePublicKey(modulusHex: String, exponentHex: String): PublicKey {
        val modulus = java.math.BigInteger(modulusHex, 16)
        val exponent = java.math.BigInteger(exponentHex, 16)
        val keySpec = RSAPublicKeySpec(modulus, exponent)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    /**
     * Encrypt a plaintext password using the given RSA public key.
     * Returns a hex-encoded ciphertext string (matching the format expected by CAS).
     */
    fun encryptPassword(plainText: String, publicKey: PublicKey): String {
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return bytesToHex(encryptedBytes)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
