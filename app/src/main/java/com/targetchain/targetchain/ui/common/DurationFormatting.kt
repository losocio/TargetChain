package com.targetchain.targetchain.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.targetchain.targetchain.R
import com.targetchain.targetchain.chain.splitDuration

/** A task's duration in words, e.g. "1 h 30 min" or "45 s". Zero parts are left out. */
@Composable
fun formatDuration(totalSeconds: Int): String {
    val (h, m, s) = splitDuration(totalSeconds.toLong())
    val parts = buildList {
        if (h > 0) add(stringResource(R.string.duration_hours, h))
        if (m > 0) add(stringResource(R.string.duration_minutes, m))
        if (s > 0 || isEmpty()) add(stringResource(R.string.duration_seconds, s))
    }
    return parts.joinToString(" ")
}
