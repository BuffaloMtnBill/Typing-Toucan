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
        com.badlogic.gdx.Gdx.input.setCatchKey(com.badlogic.gdx.Input.Keys.BACK, true)
        batch = SpriteBatch()
        soundManager = com.typingtoucan.systems.SoundManager()

        // 1. Queue all assets in AssetManager
        
        // Atlas
        assetManager.load("assets/atlas/game.atlas", com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java)

        // Textures
        for (i in 0..9) {
            assetManager.load("assets/background_tiles/tile_$i.png", Texture::class.java)
        }
        assetManager.load("assets/title_background.png", Texture::class.java)
        assetManager.load("assets/victory_background.png", Texture::class.java)
        assetManager.load("assets/minnowbyte.png", Texture::class.java)
        assetManager.load("assets/keyboard.png", Texture::class.java)

        // Audio (SFX)
        val sounds = listOf(
            "sfx_flap1.mp3", "sfx_flap2.mp3", "sfx_flap3.mp3", "sfx_flap4.mp3", "sfx_flap.mp3",
            "sfx_score1.mp3", "sfx_score2.mp3", "sfx_score3.mp3", "sfx_score4.mp3", "sfx_score.mp3",
            "sfx_score1.wav", "sfx_score2.wav", "sfx_score3.wav", "sfx_score4.wav", "sfx_score.wav",
            "sfx_monkey1.mp3", "sfx_monkey2.mp3", "sfx_monkey3.mp3",
            "sfx_crash.mp3", "sfx_levelup.mp3", "sfx_levelup_practice.mp3", "sfx_error.mp3", 
            "minnowbyte.mp3"
        )
        sounds.forEach { path ->
            if (com.badlogic.gdx.Gdx.files.internal("assets/$path").exists()) {
                assetManager.load("assets/$path", com.badlogic.gdx.audio.Sound::class.java)
            }
        }

        // Audio (Music)
        assetManager.load("assets/music_bg.mp3", com.badlogic.gdx.audio.Music::class.java)
        assetManager.load("assets/music_dark_forest.mp3", com.badlogic.gdx.audio.Music::class.java)

        // 2. Link SoundManager to AssetManager (it will pull assets as they load)
        soundManager.init(assetManager)

        // 3. Initialize the splash screen
        setScreen(com.typingtoucan.screens.SplashScreen(this))
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
