package com.targetchain.targetchain.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.targetchain.targetchain.data.model.Alarm
import com.targetchain.targetchain.data.model.Challenge
import com.targetchain.targetchain.data.model.Routine
import com.targetchain.targetchain.data.model.RoutineWithAlarms
import com.targetchain.targetchain.data.model.TaskDraft
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Insert
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutine(routineId: Long): Routine?

    @Query("UPDATE routines SET isEnabled = :isEnabled WHERE id = :routineId")
    suspend fun setRoutineEnabled(routineId: Long, isEnabled: Boolean)

    /** Its alarms and their challenges are deleted too (cascade). */
    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: Long)

    @Transaction
    @Query("SELECT * FROM routines")
    fun observeRoutinesWithAlarms(): Flow<List<RoutineWithAlarms>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineWithAlarms(routineId: Long): RoutineWithAlarms?

    @Insert
    suspend fun insertAlarm(alarm: Alarm): Long

    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarm(alarmId: Long): Alarm?

    @Query("SELECT * FROM alarms WHERE routineId = :routineId ORDER BY position")
    suspend fun getTasks(routineId: Long): List<Alarm>

    @Query("DELETE FROM alarms WHERE routineId = :routineId")
    suspend fun deleteTasks(routineId: Long)

    @Query("SELECT * FROM challenges WHERE alarmId = :alarmId ORDER BY position")
    suspend fun getChallenges(alarmId: Long): List<Challenge>

    @Insert
    suspend fun insertChallenges(challenges: List<Challenge>)

    /**
     * Creates (when [routineId] is null or no longer exists) or updates an enabled routine,
     * replacing all its tasks and their challenges with [tasks] in that order. The first task gets
     * [startTimeMinutes]. Returns the saved tasks in order.
     */
    @Transaction
    suspend fun saveRoutine(
        routineId: Long?,
        name: String,
        startTimeMinutes: Int,
        tasks: List<TaskDraft>,
    ): List<Alarm> {
        val existingRoutine = routineId?.let { getRoutine(it) }
        val savedRoutineId = if (existingRoutine != null) {
            updateRoutine(existingRoutine.copy(name = name, isEnabled = true))
            existingRoutine.id
        } else {
            insertRoutine(Routine(name = name, isEnabled = true))
        }

        // Their challenges are deleted too (cascade).
        deleteTasks(savedRoutineId)
        return tasks.mapIndexed { position, task ->
            val alarm = Alarm(
                routineId = savedRoutineId,
                position = position,
                name = task.name,
                startTimeMinutes = if (position == 0) startTimeMinutes else null,
                durationSeconds = task.durationSeconds,
                ringtoneUri = task.ringtoneUri,
            )
            val saved = alarm.copy(id = insertAlarm(alarm))
            insertChallenges(
                task.challenges.mapIndexed { challengePosition, config ->
                    Challenge(
                        alarmId = saved.id,
                        position = challengePosition,
                        type = config.type,
                        passwordId = config.passwordId,
                        expectedValue = config.expectedValue,
                    )
                }
            )
            saved
        }
    }

    /** First task of every enabled routine; used to reschedule after reboot or time change. */
    @Query(
        """
        SELECT a.* FROM alarms a
        JOIN routines r ON r.id = a.routineId
        WHERE r.isEnabled = 1
          AND a.position = (SELECT MIN(position) FROM alarms WHERE routineId = r.id)
        """
    )
    suspend fun getFirstAlarmsOfEnabledRoutines(): List<Alarm>
}
