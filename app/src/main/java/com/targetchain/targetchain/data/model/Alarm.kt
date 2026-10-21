package com.targetchain.targetchain.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One alarm of a routine chain. Each alarm marks the start of an activity.
 *
 * @property startTimeMinutes time of day (minutes since midnight) at which the alarm rings.
 *   Used by the first alarm of the chain; later alarms ring when the previous activity ends.
 * @property activityDurationMinutes countdown that starts once this alarm's challenges are
 *   completed. Null means the routine ends after this alarm.
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
    val startTimeMinutes: Int? = null,
    val activityDurationMinutes: Int? = null,
    val ringtoneUri: String? = null,
)
