package com.typingtoucan.systems

/**
 * Manages game difficulty settings and associated parameters.
 *
 * Provides varying game physics and progression speeds based on the selected [Difficulty] level.
 *
 * @property difficulty The current difficulty level.
 */
class DifficultyManager(val difficulty: Difficulty) {
    /**
     * Enumeration of available difficulty levels with their specific configuration values.
     *
     * @property scrollSpeed The horizontal scrolling speed of the world.
     * @property neckInterval The time interval between spawning obstacles (necks).
     * @property gravity The gravitational force applied to the bird per frame.
     * @property flapStrength The upward force applied when the bird flaps.
     */
    enum class Difficulty(
            val scrollSpeed: Float,
            val neckInterval: Float,
            val gravity: Float,
            val flapStrength: Float
    ) {
        EASY(150f, 2.5f, -0.08f, 3.25f),
        NORMAL(200f, 2.0f, -0.5f, 10.0f),
        HARD(300f, 1.5f, -0.7f, 10.0f),
        INSANE(400f, 1.0f, -0.9f, 12.0f)
    }

    /** The current scroll speed derived from the difficulty level. */
    val scrollSpeed: Float
        get() = difficulty.scrollSpeed
        
    /** The current neck spawn interval derived from the difficulty level. */
    val neckInterval: Float
        get() = difficulty.neckInterval
        
    /** The current gravity derived from the difficulty level. */
    val gravity: Float
        get() = difficulty.gravity
        
    /** The current flap strength derived from the difficulty level. */
    val flapStrength: Float
        get() = difficulty.flapStrength
}
