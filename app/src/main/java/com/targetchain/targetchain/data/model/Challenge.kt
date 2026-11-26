package com.targetchain.targetchain.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A challenge that must be completed to dismiss a task's alarm. A task's challenges are shown one
 * by one, ordered by [position].
 *
 * @property passwordId saved password of a [ChallengeType.PASSWORD] challenge. A password can't be
 *   deleted while a challenge uses it (RESTRICT).
 * @property expectedValue for the challenge types still to come: the scanned code or the NFC tag id.
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
        ForeignKey(
            entity = SavedPassword::class,
            parentColumns = ["id"],
            childColumns = ["passwordId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("alarmId"), Index("passwordId")],
)
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alarmId: Long,
    val position: Int,
    val type: ChallengeType,
    val passwordId: Long? = null,
    val expectedValue: String? = null,
)

/** A challenge as configured in the editor, before it belongs to a saved task. */
data class ChallengeConfig(
    val type: ChallengeType,
    val passwordId: Long? = null,
    val expectedValue: String? = null,
)

fun Challenge.toConfig() = ChallengeConfig(type, passwordId, expectedValue)
