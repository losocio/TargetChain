package com.targetchain.targetchain.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.R
import com.targetchain.targetchain.data.model.ChallengeConfig
import com.targetchain.targetchain.ui.routineedit.RoutineEditViewModel

/** The editing task's challenges in order. Changes are part of the routine edit (saved with Save). */
@Composable
fun ChallengeListScreen(
    viewModel: RoutineEditViewModel,
    onAddChallenge: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChallengeListContent(
        challenges = uiState.editingTask.challenges,
        passwordNames = uiState.passwordNames,
        onAddChallenge = onAddChallenge,
        onMove = viewModel::onChallengeMoved,
        onDelete = viewModel::onChallengeDeleted,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeListContent(
    challenges: List<ChallengeConfig>,
    passwordNames: Map<Long, String>,
    onAddChallenge: () -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onDelete: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.challenge_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.navigate_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(challenges) { index, challenge ->
                ChallengeRow(
                    position = index + 1,
                    challenge = challenge,
                    passwordNames = passwordNames,
                    canMoveUp = index > 0,
                    canMoveDown = index < challenges.lastIndex,
                    onMoveUp = { onMove(index, index - 1) },
                    onMoveDown = { onMove(index, index + 1) },
                    onDelete = { onDelete(index) },
                )
            }
            item {
                Button(onClick = onAddChallenge, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.challenge_add))
                }
            }
        }
    }
}

@Composable
private fun ChallengeRow(
    position: Int,
    challenge: ChallengeConfig,
    passwordNames: Map<Long, String>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.challenge_row_title, position, stringResource(challenge.type.labelRes())),
                    style = MaterialTheme.typography.titleMedium,
                )
                challenge.detail(passwordNames)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(painterResource(R.drawable.ic_arrow_upward), contentDescription = stringResource(R.string.challenge_move_up))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(painterResource(R.drawable.ic_arrow_downward), contentDescription = stringResource(R.string.challenge_move_down))
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.challenge_delete))
            }
        }
    }
}
