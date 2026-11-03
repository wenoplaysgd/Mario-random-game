package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.audio.RetroSoundManager
import com.example.data.GamePreferences
import com.example.ui.GameScreen
import com.example.ui.LevelSelectScreen
import com.example.ui.theme.MyApplicationTheme

sealed class Screen {
    data object LevelSelect : Screen()
    data class Game(val levelId: Int) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: RetroSoundManager
    private lateinit var preferences: GamePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        soundManager = RetroSoundManager(this)
        preferences = GamePreferences(this)
        soundManager.isSoundEnabled = preferences.isSoundEnabled
        soundManager.isHapticsEnabled = preferences.isHapticsEnabled

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.LevelSelect) }

                    when (val screen = currentScreen) {
                        is Screen.LevelSelect -> {
                            LevelSelectScreen(
                                preferences = preferences,
                                onSelectLevel = { id ->
                                    currentScreen = Screen.Game(id)
                                }
                            )
                        }
                        is Screen.Game -> {
                            GameScreen(
                                levelId = screen.levelId,
                                soundManager = soundManager,
                                preferences = preferences,
                                onBackToLevelSelect = {
                                    currentScreen = Screen.LevelSelect
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
