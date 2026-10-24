package com.targetchain.targetchain.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.targetchain.targetchain.ui.alarmedit.AlarmEditScreen
import com.targetchain.targetchain.ui.alarmedit.AlarmEditViewModel
import com.targetchain.targetchain.ui.alarmlist.AlarmListScreen
import com.targetchain.targetchain.ui.alarmlist.AlarmListViewModel
import com.targetchain.targetchain.ui.ringtone.RingtonePickerScreen
import com.targetchain.targetchain.ui.ringtone.RingtonePickerViewModel

/** String routes; argument names are the ViewModels' SavedStateHandle keys. */
private object Routes {
    const val ALARM_LIST = "alarms"

    const val ALARM_EDIT = "alarm/{${AlarmEditViewModel.ARG_ROUTINE_ID}}"
    fun alarmEdit(routineId: Long) = "alarm/$routineId"

    const val RINGTONE_PICKER = "ringtone?${RingtonePickerViewModel.ARG_SELECTED_URI}={${RingtonePickerViewModel.ARG_SELECTED_URI}}"
    fun ringtonePicker(selectedUri: String?) =
        if (selectedUri == null) "ringtone" else "ringtone?${RingtonePickerViewModel.ARG_SELECTED_URI}=${Uri.encode(selectedUri)}"
}

/**
 * Key of the ringtone picked in the picker, set on the editor's back stack entry. An empty string
 * means the default tone, since a null value can't be told apart from "nothing picked".
 */
private const val PICKED_RINGTONE_KEY = "pickedRingtone"

@Composable
fun TargetChainNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.ALARM_LIST) {
        composable(Routes.ALARM_LIST) { entry ->
            AlarmListScreen(
                viewModel = viewModel(factory = AlarmListViewModel.Factory),
                onAddAlarm = {
                    entry.ifResumed { navController.navigate(Routes.alarmEdit(AlarmEditViewModel.NEW_ROUTINE_ID)) }
                },
                onOpenAlarm = { routineId ->
                    entry.ifResumed { navController.navigate(Routes.alarmEdit(routineId)) }
                },
            )
        }

        composable(
            Routes.ALARM_EDIT,
            arguments = listOf(navArgument(AlarmEditViewModel.ARG_ROUTINE_ID) { type = NavType.LongType }),
        ) { entry ->
            val viewModel: AlarmEditViewModel = viewModel(factory = AlarmEditViewModel.Factory)

            val pickedRingtone by entry.savedStateHandle
                .getStateFlow<String?>(PICKED_RINGTONE_KEY, null)
                .collectAsStateWithLifecycle()
            LaunchedEffect(pickedRingtone) {
                pickedRingtone?.let {
                    viewModel.onRingtonePicked(it.ifEmpty { null })
                    entry.savedStateHandle.remove<String>(PICKED_RINGTONE_KEY)
                }
            }

            AlarmEditScreen(
                viewModel = viewModel,
                onPickRingtone = { selectedUri ->
                    entry.ifResumed { navController.navigate(Routes.ringtonePicker(selectedUri)) }
                },
                onClose = { entry.ifResumed { navController.popBackStack() } },
            )
        }

        composable(
            Routes.RINGTONE_PICKER,
            arguments = listOf(
                navArgument(RingtonePickerViewModel.ARG_SELECTED_URI) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            RingtonePickerScreen(
                viewModel = viewModel(factory = RingtonePickerViewModel.Factory),
                onConfirm = { selectedUri ->
                    entry.ifResumed {
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(PICKED_RINGTONE_KEY, selectedUri.orEmpty())
                        navController.popBackStack()
                    }
                },
                onBack = { entry.ifResumed { navController.popBackStack() } },
            )
        }
    }
}

/**
 * Runs [action] only while this destination is the resumed one. A second tap during the
 * navigation animation would otherwise navigate or pop twice.
 */
private fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
