package com.targetchain.targetchain.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A reusable password for password challenges. Only its salted hash is stored: after creation the
 * app shows only its [name], which is unique (ignoring case).
 */
@Entity(
    tableName = "passwords",
    indices = [Index(value = ["name"], unique = true)],
)
class SavedPassword(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val hash: ByteArray,
    val salt: ByteArray,
)

/** What the UI may know about a saved password: never its hash. */
data class PasswordSummary(val id: Long, val name: String)
