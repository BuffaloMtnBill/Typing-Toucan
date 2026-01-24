package com.typingtoucan.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

/**
 * Manages the loading, playing, and lifecycle of all audio assets in the game.
 *
 * Handles both short-duration sound effects and long-duration background music.
 * Supports enabling/disabling of sound and music separateley.
 */
class SoundManager {
    private val flapSounds = com.badlogic.gdx.utils.Array<Sound>()
    private val scoreSounds = com.badlogic.gdx.utils.Array<Sound>()
    private val monkeySounds = com.badlogic.gdx.utils.Array<Sound>()
    private var crashSound: Sound? = null
    private var levelUpSound: Sound? = null
    private var levelUpPracticeSound: Sound? = null
    private var errorSound: Sound? = null
    private var splashSound: Sound? = null
    private var bgMusic: Music? = null

    /** Controls whether sound effects are played. */
    var soundEnabled: Boolean = true
    
    /** Controls whether background music is played. Updates playback immediately. */
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                playMusic()
            } else {
                stopMusic()
            }
        }

    /** Available music tracks. */
    enum class MusicTrack {
        WHAT,
        DARK_FOREST
    }

    /** The currently selected background music track. Switches track immediately upon change. */
    var currentTrack: MusicTrack = MusicTrack.WHAT
        set(value) {
            if (field != value) {
                field = value
                switchMusic(value)
            }
        }
    
    /** The track selected by the user in the menu, to be applied when the game starts. */
    var pendingTrack: MusicTrack = MusicTrack.WHAT

    private var assetManager: com.badlogic.gdx.assets.AssetManager? = null

    /**
     * Links the SoundManager to an AssetManager to retrieve preloaded audio assets.
     */
    fun init(am: com.badlogic.gdx.assets.AssetManager) {
        this.assetManager = am
        
        // Populate collections from AssetManager
        // FLAP
        flapSounds.clear()
        for (i in 1..4) {
            val s = getSound("assets/sfx_flap$i.mp3")
            if (s != null) flapSounds.add(s)
        }
        if (flapSounds.size == 0) {
            val s = getSound("assets/sfx_flap.mp3")
            if (s != null) flapSounds.add(s)
        }

        // SCORE
        scoreSounds.clear()
        for (i in 1..4) {
            var s = getSound("assets/sfx_score$i.mp3")
            if (s == null) s = getSound("assets/sfx_score$i.wav")
            if (s != null) scoreSounds.add(s)
        }
        if (scoreSounds.size == 0) {
            var s = getSound("assets/sfx_score.mp3")
            if (s == null) s = getSound("assets/sfx_score.wav")
            if (s != null) scoreSounds.add(s)
        }

        // MONKEY
        monkeySounds.clear()
        for (i in 1..3) {
            val s = getSound("assets/sfx_monkey$i.mp3")
            if (s != null) monkeySounds.add(s)
        }

        crashSound = getSound("assets/sfx_crash.mp3")
        levelUpSound = getSound("assets/sfx_levelup.mp3")
        levelUpPracticeSound = getSound("assets/sfx_levelup_practice.mp3")
        errorSound = getSound("assets/sfx_error.mp3")
        splashSound = getSound("assets/minnowbyte.mp3")

        // Initial Music (managed individually via loadMusic)
        val path = when (currentTrack) {
            MusicTrack.WHAT -> "assets/music_bg.mp3"
            MusicTrack.DARK_FOREST -> "assets/music_dark_forest.mp3"
        }
        bgMusic = loadMusic(path)
    }

    private fun getSound(path: String): Sound? {
        val am = assetManager ?: return null
        return if (am.isLoaded(path, Sound::class.java)) {
            am.get(path, Sound::class.java)
        } else {
            null
        }
    }


    private fun loadMusic(path: String): Music? {
        return try {
            if (Gdx.files.internal(path).exists()) {
                Gdx.audio.newMusic(Gdx.files.internal(path))
            } else {
                Gdx.app.log("SoundManager", "Music file not found: $path")
                null
            }
        } catch (e: Exception) {
            Gdx.app.error("SoundManager", "Error loading music: $path", e)
            null
        }
    }

    /** Plays a random flap sound effect. */
    fun playFlap() {
        if (soundEnabled && flapSounds.size > 0) {
            flapSounds.random().play(0.5f)
        }
    }

    /** Plays a random score sound effect. */
    fun playScore() {
        if (soundEnabled && scoreSounds.size > 0) {
            scoreSounds.random().play(0.5f)
        }
    }

    /** Plays a random monkey sound effect. */
    fun playMonkey() {
        if (soundEnabled && monkeySounds.size > 0) {
            val id = monkeySounds.random().play(0.6f)
            // Optional: Randomize pitch slightly?
            // monkeySounds.random().setPitch(id, 0.9f + Math.random().toFloat() * 0.2f) 
        }
    }

    /** Plays the crash sound effect. */
    fun playCrash() {
        if (soundEnabled) crashSound?.play(0.7f)
    }

    /** Plays the primary level up sound. */
    fun playLevelUp() {
        if (soundEnabled) levelUpSound?.play(1.0f) // Loud and proud
    }

    /** Plays the practice mode level up / variation sound. */
    fun playLevelUpPractice() {
        if (soundEnabled) levelUpPracticeSound?.play(1.0f)
    }

    /** Plays the error sound effect. */
    fun playError() {
        if (soundEnabled) errorSound?.play(0.6f)
    }

    /** Plays the menu selection sound (score1). */
    fun playMenuSelect() {
        if (soundEnabled && scoreSounds.size > 0) {
            scoreSounds[0].play(0.5f)
        }
    }

    /** Plays the MinnowByte splash sound. */
    fun playSplash() {
        if (soundEnabled) splashSound?.play(1.0f)
    }

    /** Starts playing the background music if enabled and not already playing. */
    fun playMusic() {
        if (!musicEnabled) return
        if (bgMusic == null) {
            // Load current track if null
            loadCurrentTrack()
        }
        bgMusic?.apply {
            if (!isPlaying) {
                isLooping = true
                volume = 0.3f
                play()
            }
        }
    }

    private fun loadCurrentTrack() {
        bgMusic?.dispose() // Dispose old if exists
        val path =
                when (currentTrack) {
                    MusicTrack.WHAT -> "assets/music_bg.mp3"
                    MusicTrack.DARK_FOREST -> "assets/music_dark_forest.mp3"
                }
        bgMusic = loadMusic(path)
    }

    private fun switchMusic(track: MusicTrack) {
        stopMusic()
        loadCurrentTrack()
        if (musicEnabled) {
            playMusic()
        }
    }

    /** Stops the currently playing background music. */
    fun stopMusic() {
        bgMusic?.stop()
    }

    /** Pauses the background music. */
    fun pauseMusic() {
        bgMusic?.pause()
    }

    /** Resumes the background music. */
    fun resumeMusic() {
        if (musicEnabled) {
            bgMusic?.play()
        }
    }

    /** Disposes of all audio assets. */
    fun dispose() {
        bgMusic?.dispose()
    }
}
