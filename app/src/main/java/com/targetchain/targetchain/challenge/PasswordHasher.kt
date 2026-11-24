package com.targetchain.targetchain.challenge

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Saved passwords are stored only as a salted PBKDF2 hash: the app never needs to show them again.
 * Passwords are compared case-sensitively, ignoring leading and trailing spaces. Changing
 * [normalize] would invalidate every saved password.
 * Hashing is slow on purpose; call it off the main thread.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun normalize(password: String): String = password.trim()

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    /** [password] must not be blank (checked when it's created). */
    fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(normalize(password).toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(password: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        if (normalize(password).isEmpty()) return false
        return MessageDigest.isEqual(hash(password, salt), expectedHash)
    }
}
