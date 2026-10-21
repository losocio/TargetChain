package com.targetchain.targetchain

import android.app.Application
import android.content.Context
import com.targetchain.targetchain.alarm.AlarmNotifications
import com.targetchain.targetchain.alarm.AlarmScheduler
import com.targetchain.targetchain.data.RoutineRepository
import com.targetchain.targetchain.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TargetChainApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AlarmNotifications.createChannel(this)
    }
}

/** Manual dependency container, created once per process. */
class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val routineRepository = RoutineRepository(AppDatabase.create(context).routineDao())
    val alarmScheduler = AlarmScheduler(context)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as TargetChainApp).container
