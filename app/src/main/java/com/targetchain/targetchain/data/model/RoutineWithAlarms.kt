package com.targetchain.targetchain.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineWithAlarms(
    @Embedded val routine: Routine,
    @Relation(parentColumn = "id", entityColumn = "routineId")
    val alarms: List<Alarm>,
)

/** The routine's tasks in order. */
val RoutineWithAlarms.tasks: List<Alarm>
    get() = alarms.sortedBy { it.position }

/** First task of the chain: the one with the routine's start time. */
val RoutineWithAlarms.firstAlarm: Alarm?
    get() = alarms.minByOrNull { it.position }
