package com.typingtoucan.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/**
 * Handles persistent data storage using [Preferences].
 *
 * Saves and loads high scores and user preferences.
 */
object SaveManager {
    private const val PREFS_NAME = "TypingToucanPrefs"
    private const val HIGH_SCORE_KEY = "highScore" // Legacy
    private val prefs: Preferences by lazy { Gdx.app.getPreferences(PREFS_NAME) }

    private const val KEY_NORMAL_LEVEL = "normal_level"
    private const val KEY_CUSTOM_STREAK = "custom_streak_v2"
    private const val KEY_TEXT_STREAK = "text_streak_v2"
    private const val KEY_ARCADE_STREAK = "arcade_streak"

    // Normal Mode (Level)
    fun saveNormalLevel(level: Int) {
        val current = getNormalLevel()
        if (level > current) {
            prefs.putInteger(KEY_NORMAL_LEVEL, level)
            prefs.flush()
        }
    }
    fun getNormalLevel(): Int = prefs.getInteger(KEY_NORMAL_LEVEL, 1)

    // Custom Mode (Streak)
    fun saveCustomStreak(streak: Int) {
        val current = getCustomStreak()
        if (streak > current) {
            prefs.putInteger(KEY_CUSTOM_STREAK, streak)
            prefs.flush()
        }
    }
    fun getCustomStreak(): Int = prefs.getInteger(KEY_CUSTOM_STREAK, 0)

    // Text Mode (Streak)
    fun saveTextStreak(streak: Int) {
        val current = getTextStreak()
        if (streak > current) {
            prefs.putInteger(KEY_TEXT_STREAK, streak)
            prefs.flush()
        }
    }
    fun getTextStreak(): Int = prefs.getInteger(KEY_TEXT_STREAK, 0)

    // Arcade Mode (Streak)
    fun saveArcadeStreak(streak: Int) {
        val current = getArcadeStreak()
        if (streak > current) {
            prefs.putInteger(KEY_ARCADE_STREAK, streak)
            prefs.flush()
        }
    }
    fun getArcadeStreak(): Int = prefs.getInteger(KEY_ARCADE_STREAK, 0)

    fun resetHighScore() {
        prefs.putInteger(HIGH_SCORE_KEY, 0)
        prefs.putInteger(KEY_NORMAL_LEVEL, 1)
        prefs.putInteger(KEY_CUSTOM_STREAK, 0)
        prefs.putInteger(KEY_TEXT_STREAK, 0)
        prefs.putInteger(KEY_ARCADE_STREAK, 0)
        prefs.flush()
    }

    private const val CAPITALS_KEY = "capitalsEnabled"

    /** Saves the user's preference for capital letters. */
    fun saveCapitalsEnabled(enabled: Boolean) {
        prefs.putBoolean(CAPITALS_KEY, enabled)
        prefs.flush()
    }

    /** Loads the user's preference for capital letters. */
    fun loadCapitalsEnabled(): Boolean {
        return prefs.getBoolean(CAPITALS_KEY, false)
    }
}
