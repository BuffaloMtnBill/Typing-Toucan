package com.typingtoucan

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

fun main() {
    val config =
            Lwjgl3ApplicationConfiguration().apply {
                setTitle("Typing Toucan")
                setWindowedMode(1600, 1200)
                useVsync(true)
                setForegroundFPS(60)
            }
    try {
        Lwjgl3Application(TypingToucanGame(), config)
    } catch (e: Throwable) {
        println("CRITICAL ERROR: Game Crashed")
        e.printStackTrace()
    }
}
