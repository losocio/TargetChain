package com.targetchain.targetchain.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One task of a routine (called "Task" in the UI): the alarm that starts it, with its tone and
 * challenges, and its duration.
 *
 * @property name task name; blank is shown as "Task <number>".
 * @property startTimeMinutes time of day (minutes since midnight) at which the alarm rings.
 *   Used by the first task only; later tasks ring when the previous task's countdown ends.
 * @property durationSeconds countdown that starts once this task's challenges are completed.
 * @property ringtoneUri tone to play; null means the system default alarm tone.
 */
@Entity(
    tableName = "alarms",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId")],
)
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val position: Int,
    val name: String = "",
    val startTimeMinutes: Int? = null,
    val durationSeconds: Int? = null,
    val ringtoneUri: String? = null,
)
