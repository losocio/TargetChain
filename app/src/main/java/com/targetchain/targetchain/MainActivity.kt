package com.targetchain.targetchain

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.targetchain.targetchain.ui.navigation.TargetChainNavHost
import com.targetchain.targetchain.ui.theme.TargetChainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // No autofill: password managers must not offer to save or fill the challenge passwords.
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        setContent {
            TargetChainTheme {
                TargetChainNavHost()
            }
        }
    }
}
