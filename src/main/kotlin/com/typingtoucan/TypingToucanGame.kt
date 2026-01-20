package com.typingtoucan

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.typingtoucan.screens.MenuScreen

/**
 * The main game class extending [Game].
 *
 * Responsible for managing global resources such as the [SpriteBatch], [SoundManager], and [AssetManager].
 * It handles the loading of all game assets at the start and manages screen transitions.
 */
class TypingToucanGame : Game() {
    /** The main sprite batch used for rendering across the application. */
    public lateinit var batch: SpriteBatch

    /** Manages sound effects and music. initialized in [create]. */
    lateinit var soundManager: com.typingtoucan.systems.SoundManager
    
    /** The central asset manager for loading and retrieving textures and sounds. */
    val assetManager = com.badlogic.gdx.assets.AssetManager()

    /**
     * Initializes the game.
     *
     * Creates the [SpriteBatch] and [SoundManager], queues all necessary assets for loading,
     * and sets the initial screen to [MenuScreen].
     */
    override fun create() {
        batch = SpriteBatch()
        soundManager = com.typingtoucan.systems.SoundManager()

        // Atlas
        assetManager.load("assets/atlas/game.atlas", com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java)

        // Backgrounds (Huge/Oversized - kept as single textures)
        assetManager.load("assets/background_panoramic.png", Texture::class.java)
        assetManager.load("assets/title_background.png", Texture::class.java)
        assetManager.load("assets/victory_background.png", Texture::class.java)

        // Block until all assets are loaded
        assetManager.finishLoading()

        setScreen(MenuScreen(this))
    }

    /**
     * Disposes of all native resources.
     *
     * Releases memory for the [batch], [soundManager], and [assetManager].
     */
    override fun dispose() {
        batch.dispose()
        soundManager.dispose()
        assetManager.dispose()
    }
}
