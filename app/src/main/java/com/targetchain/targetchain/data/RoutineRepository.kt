package com.targetchain.targetchain.data

import com.targetchain.targetchain.data.db.RoutineDao
import com.targetchain.targetchain.data.model.Alarm
import com.targetchain.targetchain.data.model.RoutineWithAlarms
import kotlinx.coroutines.flow.Flow

/**
 * Data access for routines and their alarms. Until chaining exists, every routine has a single
 * alarm, and the UI presents each routine as one alarm.
 */
class RoutineRepository(private val dao: RoutineDao) {

    fun observeRoutines(): Flow<List<RoutineWithAlarms>> = dao.observeRoutinesWithAlarms()

    suspend fun getRoutine(routineId: Long): RoutineWithAlarms? = dao.getRoutineWithAlarms(routineId)

    /** Creates (when [routineId] is null) or updates a single-alarm routine and enables it. */
    suspend fun saveSingleAlarmRoutine(
        routineId: Long?,
        name: String,
        startTimeMinutes: Int,
        ringtoneUri: String?,
    ): Alarm = dao.saveSingleAlarmRoutine(routineId, name, startTimeMinutes, ringtoneUri)

    suspend fun setRoutineEnabled(routineId: Long, isEnabled: Boolean) =
        dao.setRoutineEnabled(routineId, isEnabled)

    suspend fun deleteRoutine(routineId: Long) = dao.deleteRoutine(routineId)

    suspend fun getAlarm(alarmId: Long): Alarm? = dao.getAlarm(alarmId)

    suspend fun disableRoutine(routineId: Long) = dao.setRoutineEnabled(routineId, false)

    suspend fun getAlarmsToReschedule(): List<Alarm> = dao.getFirstAlarmsOfEnabledRoutines()
}
