package com.typingtoucan.systems

class DifficultyManager(val difficulty: Difficulty) {
    enum class Difficulty(
            val scrollSpeed: Float,
            val neckInterval: Float,
            val gravity: Float,
            val flapStrength: Float
    ) {
        EASY(150f, 2.5f, -0.08f, 3.25f),
        NORMAL(200f, 2.0f, -0.5f, 10.0f),
        HARD(300f, 1.5f, -0.7f, 10.0f),
        INSANE(400f, 1.0f, -0.9f, 10.0f)
    }

    val scrollSpeed: Float
        get() = difficulty.scrollSpeed
    val neckInterval: Float
        get() = difficulty.neckInterval
    val gravity: Float
        get() = difficulty.gravity
    val flapStrength: Float
        get() = difficulty.flapStrength
}
