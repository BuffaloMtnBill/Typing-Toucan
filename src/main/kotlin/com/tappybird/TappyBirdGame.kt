package com.tappybird

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.tappybird.screens.MenuScreen

class TappyBirdGame : Game() {
    lateinit var batch: SpriteBatch
    val typingQueue = com.tappybird.systems.TypingQueue()
    lateinit var soundManager: com.tappybird.systems.SoundManager
    val assetManager = com.badlogic.gdx.assets.AssetManager()

    override fun create() {
        batch = SpriteBatch()
        soundManager = com.tappybird.systems.SoundManager()

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
        for (i in 0..3) assetManager.load("assets/giraffe_long_$i.png", Texture)
        // Anacondas
        for (i in 0..1) assetManager.load("assets/anaconda_long_$i.png", Texture)
        // Toucan
        for (i in 0..3) assetManager.load("assets/toucan_$i.png", Texture)
        assetManager.load("assets/toucan_pain.png", Texture)

        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        soundManager.dispose()
        assetManager.dispose()
    }
}
