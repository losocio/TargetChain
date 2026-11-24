package com.targetchain.targetchain.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    private val salt = PasswordHasher.newSalt()
    private val hash = PasswordHasher.hash("Blue Desk 42", salt)

    @Test
    fun `correct password matches`() {
        assertTrue(PasswordHasher.matches("Blue Desk 42", salt, hash))
    }

    @Test
    fun `wrong password does not match`() {
        assertFalse(PasswordHasher.matches("Blue Desk 43", salt, hash))
    }

    @Test
    fun `comparison is case sensitive`() {
        assertFalse(PasswordHasher.matches("blue desk 42", salt, hash))
    }

    @Test
    fun `leading and trailing spaces are ignored but inner spaces are kept`() {
        assertTrue(PasswordHasher.matches("  Blue Desk 42 ", salt, hash))
        assertFalse(PasswordHasher.matches("BlueDesk42", salt, hash))
    }

    @Test
    fun `blank input never matches`() {
        assertFalse(PasswordHasher.matches("   ", salt, hash))
    }

    @Test
    fun `different salts give different hashes`() {
        val otherHash = PasswordHasher.hash("Blue Desk 42", PasswordHasher.newSalt())
        assertFalse(hash.contentEquals(otherHash))
    }

    @Test
    fun `new password must not be blank and both copies must match`() {
        assertEquals(NewPasswordError.EMPTY, validateNewPassword("  ", "  "))
        assertEquals(NewPasswordError.MISMATCH, validateNewPassword("abc", "abd"))
        assertEquals(NewPasswordError.MISMATCH, validateNewPassword("abc", "ABC"))
        assertNull(validateNewPassword("abc", " abc "))
    }

    @Test
    fun `password name must not be blank or repeated ignoring case and spaces`() {
        val others = listOf("Desk", "Kitchen")
        assertEquals(PasswordNameError.EMPTY, validatePasswordName("  ", others))
        assertEquals(PasswordNameError.DUPLICATE, validatePasswordName(" desk ", others))
        assertNull(validatePasswordName("Office", others))
    }
}
