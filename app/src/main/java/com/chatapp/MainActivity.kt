package com.chatapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatapp.domain.repository.SettingsRepository
import com.chatapp.ui.navigation.NavGraph
import com.chatapp.ui.theme.ChatAppTheme
import com.chatapp.ui.theme.EN
import com.chatapp.ui.theme.LocalStrings
import com.chatapp.ui.theme.ZH
import com.chatapp.util.DebugLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DebugLog.log("APP", "onCreate version=1.0.0")
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.getThemeMode()
                .collectAsStateWithLifecycle(initialValue = "system")
            val language by settingsRepository.getLanguage()
                .collectAsStateWithLifecycle(initialValue = "en")
            ChatAppTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalStrings provides if (language == "zh") ZH else EN) {
                    NavGraph()
                }
            }
        }
    }
}
