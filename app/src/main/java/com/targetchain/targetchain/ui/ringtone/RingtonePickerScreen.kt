package com.targetchain.targetchain.ui.ringtone

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.R
import com.targetchain.targetchain.ringtone.RingtoneOption

@Composable
fun RingtonePickerScreen(
    viewModel: RingtonePickerViewModel,
    onConfirm: (selectedUri: String?) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The preview stops when the screen is left or the app goes to the background.
    LifecycleStartEffect(Unit) {
        onStopOrDispose { viewModel.onStopPreview() }
    }

    RingtonePickerContent(
        uiState = uiState,
        onSelect = viewModel::onSelect,
        onConfirm = { onConfirm(uiState.selectedUri) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RingtonePickerContent(
    uiState: RingtonePickerUiState,
    onSelect: (String?) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ringtone_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.navigate_back))
                    }
                },
                actions = {
                    TextButton(onClick = onConfirm, enabled = !uiState.isLoading) {
                        Text(stringResource(R.string.dialog_ok))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .selectableGroup(),
        ) {
            items(uiState.ringtones) { ringtone ->
                RingtoneRow(
                    ringtone = ringtone,
                    selected = ringtone.uri == uiState.selectedUri,
                    onClick = { onSelect(ringtone.uri) },
                )
            }
        }
    }
}

@Composable
private fun RingtoneRow(ringtone: RingtoneOption, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(ringtone.title) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}
