package com.typingtoucan

import com.badlogic.gdx.Game
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

        // Queue Assets
        val Texture = com.badlogic.gdx.graphics.Texture::class.java

        // Backgrounds
        assetManager.load("assets/background_panoramic.png", Texture)
        assetManager.load("assets/title_background.png", Texture)

        // Ground
        assetManager.load("assets/ground.png", Texture)
        assetManager.load("assets/victory_background.png", Texture)

        // Monkeys (9 variants)
        assetManager.load("assets/banana_monkey_1.png", Texture)
        assetManager.load("assets/banana_monkey_2.png", Texture)
        assetManager.load("assets/banana_monkey_3.png", Texture)
        assetManager.load("assets/young_monkey_1.png", Texture)
        assetManager.load("assets/young_monkey_2.png", Texture)
        assetManager.load("assets/young_monkey_3.png", Texture)
        assetManager.load("assets/old_monkey_1.png", Texture)
        assetManager.load("assets/old_monkey_2.png", Texture)
        assetManager.load("assets/old_monkey_3.png", Texture)

        // Giraffes
        for (i in 1..5) assetManager.load("assets/giraffe$i.png", Texture)
        // Anacondas
        for (i in 0..1) assetManager.load("assets/anaconda_long_$i.png", Texture)
        // Toucan
        for (i in 0..3) assetManager.load("assets/toucan_$i.png", Texture)
        assetManager.load("assets/toucan_pain.png", Texture)

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
