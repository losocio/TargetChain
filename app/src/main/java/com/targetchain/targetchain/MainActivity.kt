package com.targetchain.targetchain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.targetchain.targetchain.ui.navigation.TargetChainNavHost
import com.targetchain.targetchain.ui.routinerun.AdvanceRoutineDialog
import com.targetchain.targetchain.ui.routinerun.AdvanceRoutineViewModel
import com.targetchain.targetchain.ui.theme.TargetChainTheme

class MainActivity : ComponentActivity() {

    private val advanceViewModel: AdvanceRoutineViewModel by viewModels { AdvanceRoutineViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // No autofill: password managers must not offer to save or fill the challenge passwords.
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            TargetChainTheme {
                TargetChainNavHost(onRequestAdvance = advanceViewModel::onRequest)

                val advanceState by advanceViewModel.uiState.collectAsStateWithLifecycle()
                advanceState.dialog?.let { dialog ->
                    AdvanceRoutineDialog(
                        dialog = dialog,
                        onConfirm = advanceViewModel::onConfirm,
                        onDismiss = advanceViewModel::onDismiss,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** The countdown notification's "Next task" / "End routine" button opens the app with its confirmation. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_CONFIRM_ADVANCE, false) == true) {
            advanceViewModel.onRequest()
            intent.removeExtra(EXTRA_CONFIRM_ADVANCE)
        }
    }

    companion object {
        private const val EXTRA_CONFIRM_ADVANCE = "com.targetchain.targetchain.extra.CONFIRM_ADVANCE"

        fun newIntent(context: Context, confirmAdvance: Boolean): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_CONFIRM_ADVANCE, confirmAdvance)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
