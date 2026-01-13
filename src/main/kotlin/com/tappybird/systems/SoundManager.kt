package com.tappybird.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

class SoundManager {
    private val flapSounds = com.badlogic.gdx.utils.Array<Sound>()
    private val scoreSounds = com.badlogic.gdx.utils.Array<Sound>()
    private var crashSound: Sound? = null
    private var levelUpSound: Sound? = null
    private var errorSound: Sound? = null
    private var bgMusic: Music? = null

    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                playMusic()
            } else {
                stopMusic()
            }
        }

    enum class MusicTrack {
        WHAT,
        DARK_FOREST
    }

    var currentTrack: MusicTrack = MusicTrack.WHAT
        set(value) {
            if (field != value) {
                field = value
                switchMusic(value)
            }
        }

    init {
        // SFX
        // FLAP: Try sfx_flap1..4, fallback to sfx_flap.mp3
        for (i in 1..4) {
            val s = loadSound("assets/sfx_flap$i.mp3")
            if (s != null) flapSounds.add(s)
        }
        if (flapSounds.size == 0) {
            val s = loadSound("assets/sfx_flap.mp3")
            if (s != null) flapSounds.add(s)
        }

        // SCORE: Try sfx_score1..4 (mp3 or wav), fallback to sfx_score.mp3/wav
        for (i in 1..4) {
            var s = loadSound("assets/sfx_score$i.mp3")
            if (s == null) s = loadSound("assets/sfx_score$i.wav")
            if (s != null) scoreSounds.add(s)
        }
        if (scoreSounds.size == 0) {
            var s = loadSound("assets/sfx_score.mp3")
            if (s == null) s = loadSound("assets/sfx_score.wav")
            if (s != null) scoreSounds.add(s)
        }

        crashSound = loadSound("assets/sfx_crash.mp3")
        levelUpSound = loadSound("assets/sfx_levelup.mp3")
        errorSound = loadSound("assets/sfx_error.mp3")

        // Music
        bgMusic = loadMusic("assets/music_bg.mp3")
    }

    private fun loadSound(path: String): Sound? {
        return try {
            if (Gdx.files.internal(path).exists()) {
                Gdx.audio.newSound(Gdx.files.internal(path))
            } else {
                Gdx.app.log("SoundManager", "Sound file not found: $path")
                null
            }
        } catch (e: Exception) {
            Gdx.app.error("SoundManager", "Error loading sound: $path", e)
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

    fun playFlap() {
        if (soundEnabled && flapSounds.size > 0) {
            flapSounds.random().play(0.5f)
        }
    }

    fun playScore() {
        if (soundEnabled && scoreSounds.size > 0) {
            scoreSounds.random().play(0.5f)
        }
    }

    fun playCrash() {
        if (soundEnabled) crashSound?.play(0.7f)
    }

    fun playLevelUp() {
        if (soundEnabled) levelUpSound?.play(1.0f) // Loud and proud
    }

    fun playError() {
        if (soundEnabled) errorSound?.play(0.6f)
    }

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

    fun stopMusic() {
        bgMusic?.stop()
    }

    fun dispose() {
        flapSounds.forEach { it.dispose() }
        scoreSounds.forEach { it.dispose() }
        crashSound?.dispose()
        levelUpSound?.dispose()
        errorSound?.dispose()
        bgMusic?.dispose()
    }
}
