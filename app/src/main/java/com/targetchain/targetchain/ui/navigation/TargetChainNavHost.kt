package com.targetchain.targetchain.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.targetchain.targetchain.data.model.ChallengeConfig
import com.targetchain.targetchain.data.model.ChallengeType
import com.targetchain.targetchain.ui.challenges.ChallengeListScreen
import com.targetchain.targetchain.ui.challenges.ChallengeTypeScreen
import com.targetchain.targetchain.ui.password.NewPasswordScreen
import com.targetchain.targetchain.ui.password.NewPasswordViewModel
import com.targetchain.targetchain.ui.password.PasswordChallengeScreen
import com.targetchain.targetchain.ui.password.PasswordChallengeViewModel
import com.targetchain.targetchain.ui.ringtone.RingtonePickerScreen
import com.targetchain.targetchain.ui.ringtone.RingtonePickerViewModel
import com.targetchain.targetchain.ui.routineedit.RoutineEditScreen
import com.targetchain.targetchain.ui.routineedit.RoutineEditViewModel
import com.targetchain.targetchain.ui.routineedit.TaskEditScreen
import com.targetchain.targetchain.ui.routinelist.RoutineListScreen
import com.targetchain.targetchain.ui.routinelist.RoutineListViewModel

/** String routes; argument names are the ViewModels' SavedStateHandle keys. */
private object Routes {
    const val ROUTINE_LIST = "routines"

    /** Nested graph with the routine editor and its sub-screens, which share one RoutineEditViewModel. */
    const val ROUTINE_EDIT_GRAPH = "routine/{${RoutineEditViewModel.ARG_ROUTINE_ID}}"
    fun routineEdit(routineId: Long) = "routine/$routineId"

    const val ROUTINE_EDITOR = "routineEditor"

    /** The task chosen in the routine editor (RoutineEditUiState.editingTaskIndex). */
    const val TASK_EDITOR = "task"

    const val RINGTONE_PICKER = "ringtone?${RingtonePickerViewModel.ARG_SELECTED_URI}={${RingtonePickerViewModel.ARG_SELECTED_URI}}"
    fun ringtonePicker(selectedUri: String?) =
        if (selectedUri == null) "ringtone" else "ringtone?${RingtonePickerViewModel.ARG_SELECTED_URI}=${Uri.encode(selectedUri)}"

    const val CHALLENGE_LIST = "challenges"
    const val CHALLENGE_TYPES = "challengeTypes"
    const val PASSWORD_CHALLENGE = "passwordChallenge"
    const val NEW_PASSWORD = "newPassword"
}

/** Key of the password just created, set on the password challenge screen's back stack entry. */
private const val CREATED_PASSWORD_KEY = "createdPassword"

/** @param onRequestAdvance asks to confirm "Next task" / "End routine" (dialog shown by MainActivity). */
@Composable
fun TargetChainNavHost(onRequestAdvance: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.ROUTINE_LIST) {
        composable(Routes.ROUTINE_LIST) { entry ->
            RoutineListScreen(
                viewModel = viewModel(factory = RoutineListViewModel.Factory),
                onAddRoutine = {
                    entry.ifResumed { navController.navigate(Routes.routineEdit(RoutineEditViewModel.NEW_ROUTINE_ID)) }
                },
                onOpenRoutine = { routineId ->
                    entry.ifResumed { navController.navigate(Routes.routineEdit(routineId)) }
                },
                onRequestAdvance = onRequestAdvance,
            )
        }

        navigation(
            startDestination = Routes.ROUTINE_EDITOR,
            route = Routes.ROUTINE_EDIT_GRAPH,
            arguments = listOf(navArgument(RoutineEditViewModel.ARG_ROUTINE_ID) { type = NavType.LongType }),
        ) {
            composable(Routes.ROUTINE_EDITOR) { entry ->
                RoutineEditScreen(
                    viewModel = entry.routineEditViewModel(navController),
                    onOpenTask = { entry.ifResumed { navController.navigate(Routes.TASK_EDITOR) } },
                    // Leaving the graph discards the unsaved edit along with its ViewModel.
                    onClose = {
                        entry.ifResumed { navController.popBackStack(Routes.ROUTINE_EDIT_GRAPH, inclusive = true) }
                    },
                )
            }

            composable(Routes.TASK_EDITOR) { entry ->
                TaskEditScreen(
                    viewModel = entry.routineEditViewModel(navController),
                    onPickRingtone = { selectedUri ->
                        entry.ifResumed { navController.navigate(Routes.ringtonePicker(selectedUri)) }
                    },
                    onOpenChallenges = { entry.ifResumed { navController.navigate(Routes.CHALLENGE_LIST) } },
                    // Back keeps the changes in the routine draft; they're saved from the routine editor.
                    onBack = { entry.ifResumed { navController.popBackStack() } },
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
                val editViewModel = entry.routineEditViewModel(navController)
                RingtonePickerScreen(
                    viewModel = viewModel(factory = RingtonePickerViewModel.Factory),
                    onConfirm = { selectedUri ->
                        entry.ifResumed {
                            editViewModel.onRingtonePicked(selectedUri)
                            navController.popBackStack()
                        }
                    },
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                )
            }

            composable(Routes.CHALLENGE_LIST) { entry ->
                ChallengeListScreen(
                    viewModel = entry.routineEditViewModel(navController),
                    onAddChallenge = { entry.ifResumed { navController.navigate(Routes.CHALLENGE_TYPES) } },
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                )
            }

            composable(Routes.CHALLENGE_TYPES) { entry ->
                ChallengeTypeScreen(
                    onTypeSelected = { type ->
                        entry.ifResumed {
                            when (type) {
                                ChallengeType.PASSWORD -> navController.navigate(Routes.PASSWORD_CHALLENGE)
                                // Not offered (moved to the TFG).
                                ChallengeType.BARCODE, ChallengeType.NFC -> Unit
                            }
                        }
                    },
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                )
            }

            composable(Routes.PASSWORD_CHALLENGE) { entry ->
                val editViewModel = entry.routineEditViewModel(navController)
                val editState by editViewModel.uiState.collectAsStateWithLifecycle()
                val viewModel: PasswordChallengeViewModel = viewModel(factory = PasswordChallengeViewModel.Factory)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                val createdPassword by entry.savedStateHandle
                    .getStateFlow<Long?>(CREATED_PASSWORD_KEY, null)
                    .collectAsStateWithLifecycle()
                LaunchedEffect(createdPassword) {
                    createdPassword?.let {
                        viewModel.onSelect(it)
                        entry.savedStateHandle.remove<Long>(CREATED_PASSWORD_KEY)
                    }
                }

                PasswordChallengeScreen(
                    uiState = uiState,
                    usedPasswordIds = editState.usedPasswordIds,
                    onSelect = viewModel::onSelect,
                    onNewPassword = { entry.ifResumed { navController.navigate(Routes.NEW_PASSWORD) } },
                    onRenameClick = viewModel::onRenameClick,
                    onRenameNameChange = viewModel::onRenameNameChange,
                    onRenameConfirm = viewModel::onRenameConfirm,
                    onDeleteClick = viewModel::onDeleteClick,
                    onDeleteConfirm = viewModel::onDeleteConfirm,
                    onDialogDismiss = viewModel::onDialogDismiss,
                    onAdd = {
                        val passwordId = uiState.selectedId
                        if (uiState.canAdd && passwordId != null) {
                            entry.ifResumed {
                                editViewModel.onChallengeAdded(ChallengeConfig(ChallengeType.PASSWORD, passwordId = passwordId))
                                // Back to the list, skipping the type selector.
                                navController.popBackStack(Routes.CHALLENGE_LIST, inclusive = false)
                            }
                        }
                    },
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                )
            }

            composable(Routes.NEW_PASSWORD) { entry ->
                NewPasswordScreen(
                    viewModel = viewModel(factory = NewPasswordViewModel.Factory),
                    onCreated = { passwordId ->
                        entry.ifResumed {
                            navController.previousBackStackEntry?.savedStateHandle?.set(CREATED_PASSWORD_KEY, passwordId)
                            navController.popBackStack()
                        }
                    },
                    onBack = { entry.ifResumed { navController.popBackStack() } },
                )
            }
        }
    }
}

/** The editor's ViewModel, shared by every screen of the routine edit graph. */
@Composable
private fun NavBackStackEntry.routineEditViewModel(navController: NavController): RoutineEditViewModel {
    val graphEntry = remember(this) { navController.getBackStackEntry(Routes.ROUTINE_EDIT_GRAPH) }
    return viewModel(viewModelStoreOwner = graphEntry, factory = RoutineEditViewModel.Factory)
}

/**
 * Runs [action] only while this destination is the resumed one. A second tap during the
 * navigation animation would otherwise navigate or pop twice.
 */
private fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}
