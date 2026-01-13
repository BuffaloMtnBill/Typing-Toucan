package com.tappybird.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

object SaveManager {
    private const val PREFS_NAME = "TappyBirdPrefs"
    private const val HIGH_SCORE_KEY = "highScore"
    
    private val prefs: Preferences by lazy { Gdx.app.getPreferences(PREFS_NAME) }

    fun saveHighScore(score: Int) {
        val current = getHighScore()
        if (score > current) {
            prefs.putInteger(HIGH_SCORE_KEY, score)
            prefs.flush()
        }
    }

    fun getHighScore(): Int {
        return prefs.getInteger(HIGH_SCORE_KEY, 0)
    }
}
