package com.typingtoucan.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.typingtoucan.TypingToucanGame

/**
 * Initial screen displayed when the application starts.
 *
 * Shows the MinnowByte logo and an optional keyboard instruction screen on mobile devices.
 * Each splash screen is displayed for a fixed duration before transitioning to the main menu.
 *
 * @param game The main game instance.
 */
class SplashScreen(val game: TypingToucanGame) : Screen {
    private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 600f) }
    private val viewport = ExtendViewport(800f, 600f, camera)
    
    private var timer = 0f
    private var stage = 0 // 0: MinnowByte, 1: Keyboard (if applicable)
    
    private lateinit var minnowTexture: Texture
    private var keyboardTexture: Texture? = null
    
    private val isMobile = Gdx.app.type == Application.ApplicationType.Android || 
                           Gdx.app.type == Application.ApplicationType.iOS

    override fun show() {
        // Queue splash textures for loading
        game.assetManager.load("assets/minnowbyte.png", Texture::class.java)
        if (isMobile) {
            game.assetManager.load("assets/keyboard.png", Texture::class.java)
        }
        
        // Play MinnowByte splash sound once
        game.soundManager.playSplash()
    }

    override fun render(delta: Float) {
        timer += delta
        game.assetManager.update() // Continue loading game assets in background
        
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        
        // Check if splash assets are loaded
        if (!game.assetManager.isLoaded("assets/minnowbyte.png")) return
        if (isMobile && !game.assetManager.isLoaded("assets/keyboard.png")) return

        // Assign once
        if (!::minnowTexture.isInitialized) {
            minnowTexture = game.assetManager.get("assets/minnowbyte.png", Texture::class.java)
            if (isMobile) {
                keyboardTexture = game.assetManager.get("assets/keyboard.png", Texture::class.java)
            }
        }

        viewport.apply()
        game.batch.projectionMatrix = camera.combined
        game.batch.begin()
        
        if (stage == 0) {
            // Scale and center MinnowByte based on viewport height to avoid squishing
            val textureWidth = minnowTexture.width.toFloat()
            val textureHeight = minnowTexture.height.toFloat()
            val scale = viewport.worldHeight / textureHeight
            val drawWidth = textureWidth * scale
            val drawX = (viewport.worldWidth - drawWidth) / 2f
            
            game.batch.draw(minnowTexture, drawX, 0f, drawWidth, viewport.worldHeight)
            
            if (timer >= 2.0f) {
                timer = 0f
                if (isMobile) {
                    stage = 1
                } else {
                    // On desktop, proceed to menu after 1s
                    finish()
                }
            }
        } else if (stage == 1) {
            // Scale and center Keyboard screen based on viewport height
            keyboardTexture?.let {
                val textureWidth = it.width.toFloat()
                val textureHeight = it.height.toFloat()
                val scale = viewport.worldHeight / textureHeight
                val drawWidth = textureWidth * scale
                val drawX = (viewport.worldWidth - drawWidth) / 2f
                
                game.batch.draw(it, drawX, 0f, drawWidth, viewport.worldHeight)
            }
            
            if (timer >= 2.0f) {
                finish()
            }
        }
        
        game.batch.end()
    }

    /**
     * Transitions to the menu screen.
     * In a production scenario, we might wait for assetManager.progress == 1f here, 
     * but the MenuScreen already handles partial asset loading gracefully.
     */
    private fun finish() {
        game.screen = MenuScreen(game)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun pause() {}

    override fun resume() {}

    override fun hide() {
        dispose()
    }

    override fun dispose() {
        // Managed by AssetManager
    }
}
