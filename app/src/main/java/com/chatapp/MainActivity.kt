package com.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatapp.domain.repository.SettingsRepository
import com.chatapp.ui.navigation.NavGraph
import com.chatapp.ui.theme.ChatAppTheme
import com.chatapp.util.DebugLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DebugLog.log("APP", "onCreate version=0.0.13-alpha")
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.getThemeMode()
                .collectAsStateWithLifecycle(initialValue = "system")
            ChatAppTheme(themeMode = themeMode) {
                NavGraph()
            }
        }
    }
}
