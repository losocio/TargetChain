package com.targetchain.targetchain.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A challenge that must be completed to dismiss an alarm.
 *
 * @property expectedValue what the user must provide: the password, the scanned code or the NFC tag id.
 */
@Entity(
    tableName = "challenges",
    foreignKeys = [
        ForeignKey(
            entity = Alarm::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("alarmId")],
)
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val position: Int,
    val type: ChallengeType,
    val expectedValue: String? = null,
)
