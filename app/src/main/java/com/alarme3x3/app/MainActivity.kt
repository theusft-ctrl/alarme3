package com.alarme3x3.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alarme3x3.app.ui.AppNavigation
import com.alarme3x3.app.ui.MainViewModel
import com.alarme3x3.app.ui.MainViewModelFactory
import com.alarme3x3.app.ui.theme.Alarme3x3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as Alarme3x3App

        setContent {
            Alarme3x3Theme {
                val vm: MainViewModel = viewModel(
                    factory = MainViewModelFactory(app.repository)
                )
                AppNavigation(viewModel = vm)
            }
        }
    }
}
