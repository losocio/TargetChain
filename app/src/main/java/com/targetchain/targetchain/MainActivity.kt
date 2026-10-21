package com.targetchain.targetchain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.targetchain.targetchain.ui.home.HomeScreen
import com.targetchain.targetchain.ui.home.HomeViewModel
import com.targetchain.targetchain.ui.theme.TargetChainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TargetChainTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        viewModel = viewModel(factory = HomeViewModel.Factory),
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
