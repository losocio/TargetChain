package com.targetchain.targetchain.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.targetchain.targetchain.data.model.Alarm
import com.targetchain.targetchain.data.model.Routine
import com.targetchain.targetchain.data.model.RoutineWithAlarms
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

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarm(alarmId: Long): Alarm?

    @Query("SELECT * FROM alarms WHERE routineId = :routineId ORDER BY position LIMIT 1")
    suspend fun getFirstAlarm(routineId: Long): Alarm?

    /**
     * Creates (when [routineId] is null or no longer exists) or updates an enabled routine with a
     * single alarm. Returns the saved alarm.
     */
    @Transaction
    suspend fun saveSingleAlarmRoutine(
        routineId: Long?,
        name: String,
        startTimeMinutes: Int,
        ringtoneUri: String?,
    ): Alarm {
        val existingRoutine = routineId?.let { getRoutine(it) }
        val savedRoutineId = if (existingRoutine != null) {
            updateRoutine(existingRoutine.copy(name = name, isEnabled = true))
            existingRoutine.id
        } else {
            insertRoutine(Routine(name = name, isEnabled = true))
        }

        val existingAlarm = getFirstAlarm(savedRoutineId)
        return if (existingAlarm != null) {
            existingAlarm.copy(startTimeMinutes = startTimeMinutes, ringtoneUri = ringtoneUri)
                .also { updateAlarm(it) }
        } else {
            Alarm(
                routineId = savedRoutineId,
                position = 0,
                startTimeMinutes = startTimeMinutes,
                ringtoneUri = ringtoneUri,
            ).let { it.copy(id = insertAlarm(it)) }
        }
    }

    /** First alarm of every enabled routine; used to reschedule after reboot or time change. */
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
