package com.targetchain.targetchain.data

import com.targetchain.targetchain.data.db.RoutineDao
import com.targetchain.targetchain.data.model.Alarm
import com.targetchain.targetchain.data.model.Challenge
import com.targetchain.targetchain.data.model.RoutineWithAlarms
import com.targetchain.targetchain.data.model.TaskDraft
import kotlinx.coroutines.flow.Flow

/**
 * Data access for routines and their tasks (each task is an [Alarm] with its challenges).
 *
 * A running routine (see [isRoutineRunning]) can't be edited, disabled or deleted by the user: the
 * methods that would do it refuse and report it.
 */
class RoutineRepository(
    private val dao: RoutineDao,
    val isRoutineRunning: (routineId: Long) -> Boolean,
) {

    fun observeRoutines(): Flow<List<RoutineWithAlarms>> = dao.observeRoutinesWithAlarms()

    suspend fun getRoutine(routineId: Long): RoutineWithAlarms? = dao.getRoutineWithAlarms(routineId)

    /** The routine's tasks in order. */
    suspend fun getTasks(routineId: Long): List<Alarm> = dao.getTasks(routineId)

    suspend fun getChallenges(alarmId: Long): List<Challenge> = dao.getChallenges(alarmId)

    /**
     * Creates (when [routineId] is null) or updates a routine with all its tasks and challenges and
     * enables it. Returns the saved tasks in order, or null, saving nothing, if the routine is running.
     */
    suspend fun saveRoutine(
        routineId: Long?,
        name: String,
        startTimeMinutes: Int,
        tasks: List<TaskDraft>,
    ): List<Alarm>? {
        if (routineId != null && isRoutineRunning(routineId)) return null
        return dao.saveRoutine(routineId, name, startTimeMinutes, tasks)
    }

    /** Returns false, changing nothing, if the routine is running. */
    suspend fun setRoutineEnabled(routineId: Long, isEnabled: Boolean): Boolean {
        if (isRoutineRunning(routineId)) return false
        dao.setRoutineEnabled(routineId, isEnabled)
        return true
    }

    /** Returns false, deleting nothing, if the routine is running. */
    suspend fun deleteRoutine(routineId: Long): Boolean {
        if (isRoutineRunning(routineId)) return false
        dao.deleteRoutine(routineId)
        return true
    }

    suspend fun getAlarm(alarmId: Long): Alarm? = dao.getAlarm(alarmId)

    /** Used when a routine ends or is skipped; not limited by [isRoutineRunning]. */
    suspend fun disableRoutine(routineId: Long) = dao.setRoutineEnabled(routineId, false)

    suspend fun getAlarmsToReschedule(): List<Alarm> = dao.getFirstAlarmsOfEnabledRoutines()
}
