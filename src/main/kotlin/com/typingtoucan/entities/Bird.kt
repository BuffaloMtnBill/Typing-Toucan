package com.typingtoucan.entities

import com.badlogic.gdx.math.Rectangle

class Bird(var x: Float, var y: Float) {
    // 400% scale per user request (40f -> 160f)
    // User Update: Width -25% (160->120), Height +25% (220->275)
    // User Update 2: Height -25% (275 -> 206.25), Width Unchanged (120)
    val width = 140f
    val height = 206f

    // Hitbox fixed at 40x30px centered (User request -10px)
    val collisionWidth = 40f
    val collisionHeight = 30f

    var velocity = 0f
    var gravity = -0.5f
    var flapStrength = 10f

    // Bounds initialized with collision size
    val bounds = Rectangle(x, y, collisionWidth, collisionHeight)

    fun update() {
        velocity += gravity
        y += velocity

        // Boundaries
        // Allow y < 0 so GameScreen can detect ground collision logic.
        // We only clamp top.

        if (y > 600 - height) {
            y = 600 - height
            velocity = 0f
        }

        // Center the collision box within the visual sprite
        val centerX = x + (width - collisionWidth) / 2
        val centerY = y + (height - collisionHeight) / 2
        bounds.setPosition(centerX, centerY)
    }

    fun flap() {
        velocity = flapStrength
    }

    fun render(
            batch: com.badlogic.gdx.graphics.g2d.SpriteBatch,
            region: com.badlogic.gdx.graphics.g2d.TextureRegion
    ) {
        val rotation = (velocity * 2).coerceIn(-45f, 45f)
        batch.draw(region, x, y, width / 2, height / 2, width, height, 1f, 1f, rotation)
    }
}
