package com.typingtoucan.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

/**
 * Handles persistent data storage using [Preferences].
 *
 * Saves and loads high scores and user preferences.
 */
object SaveManager {
    private const val PREFS_NAME = "TappyBirdPrefs"
    private const val HIGH_SCORE_KEY = "highScore"

    private val prefs: Preferences by lazy { Gdx.app.getPreferences(PREFS_NAME) }

    /**
     * Updates the high score if the provided score is higher than the stored one.
     *
     * @param score The new score to check.
     */
    fun saveHighScore(score: Int) {
        val current = getHighScore()
        if (score > current) {
            prefs.putInteger(HIGH_SCORE_KEY, score)
            prefs.flush()
        }
    }

    /** Returns the current high score. */
    fun getHighScore(): Int {
        return prefs.getInteger(HIGH_SCORE_KEY, 0)
    }

    /** Resets the high score to 0. */
    fun resetHighScore() {
        prefs.putInteger(HIGH_SCORE_KEY, 0)
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
