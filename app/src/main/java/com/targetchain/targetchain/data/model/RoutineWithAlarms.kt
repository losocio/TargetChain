package com.targetchain.targetchain.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineWithAlarms(
    @Embedded val routine: Routine,
    @Relation(parentColumn = "id", entityColumn = "routineId")
    val alarms: List<Alarm>,
)

/** First alarm of the chain. While routines have a single alarm, it is the only one. */
val RoutineWithAlarms.firstAlarm: Alarm?
    get() = alarms.minByOrNull { it.position }
