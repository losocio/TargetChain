package com.targetchain.targetchain.challenge

enum class PasswordNameError { EMPTY, DUPLICATE }

enum class NewPasswordError { EMPTY, MISMATCH }

/**
 * A password name can't be blank or repeat another one. Names are compared trimmed and ignoring
 * case, so "Desk" and " desk" are the same name.
 */
fun validatePasswordName(name: String, otherNames: Collection<String>): PasswordNameError? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return PasswordNameError.EMPTY
    if (otherNames.any { it.trim().equals(trimmed, ignoreCase = true) }) return PasswordNameError.DUPLICATE
    return null
}

/** The password, typed twice, must match and not be blank (after [PasswordHasher.normalize]). */
fun validateNewPassword(password: String, repeated: String): NewPasswordError? {
    val normalized = PasswordHasher.normalize(password)
    if (normalized.isEmpty()) return NewPasswordError.EMPTY
    if (normalized != PasswordHasher.normalize(repeated)) return NewPasswordError.MISMATCH
    return null
}
