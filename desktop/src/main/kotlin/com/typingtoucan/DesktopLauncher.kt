package com.typingtoucan

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

/**
 * Main entry point for the desktop application.
 *
 * Configures the LWJGL3 application window and initializes the [TypingToucanGame].
 *
 * @param args Command line arguments (unused).
 */
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
