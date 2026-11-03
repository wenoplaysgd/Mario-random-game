package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.LevelProgress

class GamePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mario_parkour_prefs", Context.MODE_PRIVATE)

    fun getLevelProgress(levelId: Int): LevelProgress {
        val isUnlocked = if (levelId == 1) true else prefs.getBoolean("level_${levelId}_unlocked", false)
        val stars = prefs.getInt("level_${levelId}_stars", 0)
        val bestTime = prefs.getFloat("level_${levelId}_best_time", 0f)
        val coins = prefs.getInt("level_${levelId}_coins", 0)
        return LevelProgress(
            levelId = levelId,
            isUnlocked = isUnlocked,
            starsEarned = stars,
            bestTimeSeconds = bestTime,
            coinsCollected = coins
        )
    }

    fun saveLevelCompletion(levelId: Int, starsEarned: Int, timeSeconds: Float, coins: Int) {
        val currentProgress = getLevelProgress(levelId)
        val bestStars = maxOf(currentProgress.starsEarned, starsEarned)
        val bestTime = if (currentProgress.bestTimeSeconds <= 0f) {
            timeSeconds
        } else {
            minOf(currentProgress.bestTimeSeconds, timeSeconds)
        }
        val bestCoins = maxOf(currentProgress.coinsCollected, coins)

        prefs.edit().apply {
            putBoolean("level_${levelId}_unlocked", true)
            putInt("level_${levelId}_stars", bestStars)
            putFloat("level_${levelId}_best_time", bestTime)
            putInt("level_${levelId}_coins", bestCoins)
            // Unlock next level automatically
            putBoolean("level_${levelId + 1}_unlocked", true)
            apply()
        }
    }

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("haptics_enabled", value).apply()

    fun resetAllProgress() {
        prefs.edit().clear().apply()
    }
}
