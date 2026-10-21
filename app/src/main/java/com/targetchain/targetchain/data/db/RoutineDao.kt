package com.targetchain.targetchain.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.targetchain.targetchain.data.model.Alarm
import com.targetchain.targetchain.data.model.Routine
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Insert
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Query("SELECT * FROM routines WHERE name = :name LIMIT 1")
    suspend fun findRoutineByName(name: String): Routine?

    @Query("UPDATE routines SET isEnabled = :isEnabled WHERE id = :routineId")
    suspend fun setRoutineEnabled(routineId: Long, isEnabled: Boolean)

    @Insert
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarm(alarmId: Long): Alarm?

    @Query("SELECT * FROM alarms WHERE routineId = :routineId ORDER BY position LIMIT 1")
    suspend fun getFirstAlarm(routineId: Long): Alarm?

    /** First alarm of the routine with this name, or null if the routine is missing or disabled. */
    @Query(
        """
        SELECT a.* FROM alarms a
        JOIN routines r ON r.id = a.routineId
        WHERE r.name = :name AND r.isEnabled = 1
        ORDER BY a.position LIMIT 1
        """
    )
    fun observeFirstAlarmOfEnabledRoutine(name: String): Flow<Alarm?>

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
