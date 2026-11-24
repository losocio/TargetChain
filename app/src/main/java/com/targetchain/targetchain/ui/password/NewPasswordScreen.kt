package com.targetchain.targetchain.ui.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.R
import com.targetchain.targetchain.ui.common.PasswordKeyboardOptions
import kotlinx.coroutines.flow.drop

/**
 * Creates a saved password: name, and the password typed twice. While creating it, the password is
 * visible and can be pasted; it's never shown again afterwards. The keyboard is told it's a
 * password field, so it doesn't learn or suggest it.
 */
@Composable
fun NewPasswordScreen(
    viewModel: NewPasswordViewModel,
    onCreated: (passwordId: Long) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val password = rememberTextFieldState()
    val repeated = rememberTextFieldState()

    LaunchedEffect(uiState.createdId) {
        uiState.createdId?.let(onCreated)
    }
    LaunchedEffect(password, repeated) {
        snapshotFlow { password.text.toString() to repeated.text.toString() }
            .drop(1)
            .collect { viewModel.onPasswordChange() }
    }
    // Autofill is disabled for the activity; cancelling also drops any "save password?" prompt.
    val autofillManager = LocalAutofillManager.current
    DisposableEffect(autofillManager) {
        onDispose { autofillManager?.cancel() }
    }

    NewPasswordContent(
        uiState = uiState,
        password = password,
        repeated = repeated,
        onNameChange = viewModel::onNameChange,
        onCreate = { viewModel.onCreate(password.text.toString(), repeated.text.toString()) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPasswordContent(
    uiState: NewPasswordUiState,
    password: TextFieldState,
    repeated: TextFieldState,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.password_new_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.password_new_explanation), style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.password_name)) },
                placeholder = { Text(stringResource(R.string.password_name_placeholder)) },
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { error -> { Text(stringResource(error.messageRes())) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedSecureTextField(
                state = password,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.password_value)) },
                isError = uiState.passwordError != null,
                textObfuscationMode = TextObfuscationMode.Visible,
                keyboardOptions = PasswordKeyboardOptions.copy(imeAction = ImeAction.Next),
            )
            OutlinedSecureTextField(
                state = repeated,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.password_repeat)) },
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { error -> { Text(stringResource(error.messageRes())) } },
                textObfuscationMode = TextObfuscationMode.Visible,
                keyboardOptions = PasswordKeyboardOptions,
                onKeyboardAction = { onCreate() },
            )

            Button(onClick = onCreate, enabled = !uiState.isCreating && uiState.createdId == null) {
                Text(stringResource(R.string.password_create))
            }
        }
    }
}
