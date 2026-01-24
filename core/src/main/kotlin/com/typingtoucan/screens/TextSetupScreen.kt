package com.typingtoucan.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.typingtoucan.TypingToucanGame
import com.typingtoucan.systems.DifficultyManager
import com.typingtoucan.systems.TextSnippetSource

/**
 * Screen used to initialize "Text Mode".
 * Automatically loads passages and starts the game.
 */
class TextSetupScreen(val game: TypingToucanGame, val difficulty: DifficultyManager.Difficulty) : Screen {

    init {
        // No font init needed if we jump straight to game
        // But we handle loading here
    }

    override fun show() {
         startTextMode()
    }

    private fun startTextMode() {
        val passages = loadPassages()
        if (passages.isNotEmpty()) {
            game.screen = GameScreen(
                game,
                difficulty,
                isPracticeMode = true,
                customSource = TextSnippetSource(passages)
            )
        } else {
             // Fallback or Error
             Gdx.app.error("TextSetupScreen", "No passages found")
             game.screen = MenuScreen(game)
        }
    }

    private fun loadPassages(): List<com.typingtoucan.systems.PassageItem> {
        val list = mutableListOf<com.typingtoucan.systems.PassageItem>()
        try {
            val file = Gdx.files.internal("assets/passages.txt")
            if (file.exists()) {
                val content = file.readString()
                val blocks = content.split("~")
                for (block in blocks) {
                    val text = block.trim()
                    if (text.isNotEmpty()) {
                        list.add(com.typingtoucan.systems.PassageItem(text, ""))
                    }
                }
            }
        } catch (e: Exception) {
            Gdx.app.error("TextSetupScreen", "Failed to load passages", e)
        }
        return list
    }

    override fun render(delta: Float) {
        // Just clear screen black/dark while transition happens
         Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
         Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    } 
    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {}
}
