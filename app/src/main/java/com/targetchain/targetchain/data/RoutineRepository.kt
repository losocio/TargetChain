package com.targetchain.targetchain.data

import com.targetchain.targetchain.data.db.RoutineDao
import com.targetchain.targetchain.data.model.Alarm
import com.targetchain.targetchain.data.model.Routine
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val dao: RoutineDao) {

    /** The single-alarm test routine, while it is enabled. Temporary until routine editing exists. */
    fun observeTestAlarm(): Flow<Alarm?> = dao.observeFirstAlarmOfEnabledRoutine(TEST_ROUTINE_NAME)

    /** Creates or updates the test routine with one alarm at [startTimeMinutes] and enables it. */
    suspend fun saveTestAlarm(startTimeMinutes: Int): Alarm {
        val routine = dao.findRoutineByName(TEST_ROUTINE_NAME)
            ?.also { dao.updateRoutine(it.copy(isEnabled = true)) }
            ?: Routine(name = TEST_ROUTINE_NAME, isEnabled = true).let { it.copy(id = dao.insertRoutine(it)) }

        val existing = dao.getFirstAlarm(routine.id)
        return if (existing != null) {
            existing.copy(startTimeMinutes = startTimeMinutes).also { dao.updateAlarm(it) }
        } else {
            Alarm(routineId = routine.id, position = 0, startTimeMinutes = startTimeMinutes)
                .let { it.copy(id = dao.insertAlarm(it)) }
        }
    }

    suspend fun disableTestRoutine() {
        dao.findRoutineByName(TEST_ROUTINE_NAME)?.let { dao.setRoutineEnabled(it.id, false) }
    }

    suspend fun getAlarm(alarmId: Long): Alarm? = dao.getAlarm(alarmId)

    suspend fun disableRoutine(routineId: Long) = dao.setRoutineEnabled(routineId, false)

    suspend fun getAlarmsToReschedule(): List<Alarm> = dao.getFirstAlarmsOfEnabledRoutines()

    private companion object {
        const val TEST_ROUTINE_NAME = "test"
    }
}
