package com.typingtoucan

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/**
 * Main entry point for the Android application.
 * 
 * Configures the Android application and initializes the [TypingToucanGame].
 * Optimized for use with external Bluetooth keyboards.
 */
class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true // Hide system bars for maximum screen space
        }
        
        // Initialize the game
        initialize(TypingToucanGame(), config)
        
        // Ensure the soft keyboard doesn't pop up automatically
        // since we are targeting physical bluetooth keyboards.
        // This is primarily managed by Android, but we can hint to the system.
    }
}
