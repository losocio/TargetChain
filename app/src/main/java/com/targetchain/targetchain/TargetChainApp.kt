package com.targetchain.targetchain

import android.app.Application
import android.content.Context
import com.targetchain.targetchain.alarm.AlarmNotifications
import com.targetchain.targetchain.alarm.AlarmScheduler
import com.targetchain.targetchain.alarm.AlarmStateStore
import com.targetchain.targetchain.alarm.RoutineRunner
import com.targetchain.targetchain.data.PasswordRepository
import com.targetchain.targetchain.data.RoutineRepository
import com.targetchain.targetchain.data.db.AppDatabase
import com.targetchain.targetchain.ringtone.RingtoneCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TargetChainApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AlarmNotifications.createChannels(this)
    }
}

/** Manual dependency container, created once per process. [context] is the Application. */
class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database = AppDatabase.create(context)
    val alarmStateStore = AlarmStateStore(context)
    val routineRepository = RoutineRepository(database.routineDao()) { routineId ->
        alarmStateStore.activeRoutineId == routineId
    }
    val passwordRepository = PasswordRepository(database.passwordDao())
    val alarmScheduler = AlarmScheduler(context)
    val routineRunner = RoutineRunner(context, alarmStateStore, routineRepository, alarmScheduler)
    val ringtoneCatalog = RingtoneCatalog(context)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as TargetChainApp).container
