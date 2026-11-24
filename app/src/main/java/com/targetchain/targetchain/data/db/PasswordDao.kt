package com.targetchain.targetchain.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.targetchain.targetchain.data.model.PasswordSummary
import com.targetchain.targetchain.data.model.SavedPassword
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {

    @Query("SELECT id, name FROM passwords ORDER BY name")
    fun observeSummaries(): Flow<List<PasswordSummary>>

    @Query("SELECT * FROM passwords WHERE id = :passwordId")
    suspend fun get(passwordId: Long): SavedPassword?

    @Query("SELECT name FROM passwords WHERE id != :excludedId")
    suspend fun getNamesExcept(excludedId: Long): List<String>

    @Insert
    suspend fun insert(password: SavedPassword): Long

    @Query("UPDATE passwords SET name = :name WHERE id = :passwordId")
    suspend fun rename(passwordId: Long, name: String)

    @Query("DELETE FROM passwords WHERE id = :passwordId")
    suspend fun delete(passwordId: Long)

    @Query("SELECT COUNT(*) FROM challenges WHERE passwordId = :passwordId")
    suspend fun countChallengesUsing(passwordId: Long): Int

    /** Names of the routines (alarms, until chaining) with a challenge that uses the password. */
    @Query(
        """
        SELECT DISTINCT r.name FROM challenges c
        JOIN alarms a ON a.id = c.alarmId
        JOIN routines r ON r.id = a.routineId
        WHERE c.passwordId = :passwordId
        """
    )
    suspend fun getRoutineNamesUsing(passwordId: Long): List<String>
}
