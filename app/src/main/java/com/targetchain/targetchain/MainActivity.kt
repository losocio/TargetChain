package com.targetchain.targetchain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.targetchain.targetchain.ui.navigation.TargetChainNavHost
import com.targetchain.targetchain.ui.theme.TargetChainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TargetChainTheme {
                TargetChainNavHost()
            }
        }
    }
}
