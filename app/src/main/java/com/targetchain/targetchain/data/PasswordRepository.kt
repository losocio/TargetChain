package com.targetchain.targetchain.data

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.targetchain.targetchain.challenge.PasswordHasher
import com.targetchain.targetchain.challenge.PasswordNameError
import com.targetchain.targetchain.challenge.validatePasswordName
import com.targetchain.targetchain.data.db.PasswordDao
import com.targetchain.targetchain.data.model.PasswordSummary
import com.targetchain.targetchain.data.model.SavedPassword
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Saved passwords, shared by all alarms. Changes apply immediately, not as part of an alarm edit.
 * Passwords are only stored hashed and are never returned.
 */
class PasswordRepository(private val dao: PasswordDao) {

    fun observePasswords(): Flow<List<PasswordSummary>> = dao.observeSummaries()

    /** [excludedId] is the password being renamed, whose current name doesn't count as a repeat. */
    suspend fun validateName(name: String, excludedId: Long? = null): PasswordNameError? =
        validatePasswordName(name, dao.getNamesExcept(excludedId ?: NO_ID))

    /**
     * Creates a password (validated with [com.targetchain.targetchain.challenge.validateNewPassword]).
     * Returns its id, or null if the name is invalid or already used.
     */
    suspend fun create(name: String, password: String): Long? {
        if (validateName(name) != null) return null
        val salt = PasswordHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password, salt) }
        return try {
            dao.insert(SavedPassword(name = name.trim(), hash = hash, salt = salt))
        } catch (e: SQLiteConstraintException) {
            null
        }
    }

    /** Returns null when renamed, or why the name was refused. */
    suspend fun rename(passwordId: Long, name: String): PasswordNameError? {
        validateName(name, passwordId)?.let { return it }
        return try {
            dao.rename(passwordId, name.trim())
            null
        } catch (e: SQLiteConstraintException) {
            PasswordNameError.DUPLICATE
        }
    }

    /** Names of the alarms that use the password; it can't be deleted while this isn't empty. */
    suspend fun getAlarmNamesUsing(passwordId: Long): List<String> = dao.getRoutineNamesUsing(passwordId)

    /** Returns false (and deletes nothing) if a saved alarm still uses the password. */
    suspend fun delete(passwordId: Long): Boolean {
        if (dao.countChallengesUsing(passwordId) > 0) return false
        return try {
            dao.delete(passwordId)
            true
        } catch (e: SQLiteConstraintException) {
            // RESTRICT foreign key: a challenge started using it meanwhile.
            false
        }
    }

    /**
     * Whether [typed] is the password. A missing password can't happen (a used password can't be
     * deleted); if it ever did, the challenge is accepted so the alarm can still be stopped.
     */
    suspend fun matches(passwordId: Long?, typed: String): Boolean {
        val password = passwordId?.let { dao.get(it) }
        if (password == null) {
            Log.w(TAG, "Password $passwordId not found; challenge accepted")
            return true
        }
        return withContext(Dispatchers.Default) {
            PasswordHasher.matches(typed, password.salt, password.hash)
        }
    }

    private companion object {
        const val TAG = "PasswordRepository"
        const val NO_ID = -1L
    }
}
