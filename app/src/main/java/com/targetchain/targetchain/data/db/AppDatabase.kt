package com.targetchain.targetchain.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.targetchain.targetchain.data.model.Alarm
import com.targetchain.targetchain.data.model.Challenge
import com.targetchain.targetchain.data.model.Routine
import com.targetchain.targetchain.data.model.SavedPassword

@Database(
    entities = [Routine::class, Alarm::class, Challenge::class, SavedPassword::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao

    abstract fun passwordDao(): PasswordDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "targetchain.db")
                // Development only: no real user data to preserve yet.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
