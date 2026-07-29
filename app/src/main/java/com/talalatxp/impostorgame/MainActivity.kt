package com.talalatxp.impostorgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.talalatxp.impostorgame.data.repository.GameRepositoryImpl
import com.talalatxp.impostorgame.presentation.GameViewModel
import com.talalatxp.impostorgame.presentation.ImpostorApp
import com.talalatxp.impostorgame.ui.theme.ImpostorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = viewModelFactory {
            initializer { GameViewModel(GameRepositoryImpl(applicationContext)) }
        }
        setContent {
            ImpostorTheme { ImpostorApp(factory) }
        }
    }
}
