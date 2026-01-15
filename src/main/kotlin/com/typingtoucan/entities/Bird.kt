package com.typingtoucan.entities

import com.badlogic.gdx.math.Rectangle

/**
 * Represents the player controllable bird entity (Toucan).
 *
 * Handles physics validation, position updates, and rendering of the bird.
 *
 * @property x The horizontal position of the bird.
 * @property y The vertical position of the bird.
 */
class Bird(var x: Float, var y: Float) {
    /** The visual width of the bird sprite. Scaled to 400% then adjusted (-25%). */
    val width = 140f
    
    /** The visual height of the bird sprite. Scaled to 400% then adjusted (+25% then -25%). */
    val height = 206f

    /** The width of the collision hitbox. */
    val collisionWidth = 40f
    
    /** The height of the collision hitbox. */
    val collisionHeight = 30f

    /** Current vertical velocity of the bird. */
    var velocity = 0f
    
    /** Gravity applied to the velocity per frame. */
    var gravity = -0.5f
    
    /** Vertical impulse applied when flapping. */
    var flapStrength = 10f

    /** Rectangle representing the collision area of the bird. */
    val bounds = Rectangle(x, y, collisionWidth, collisionHeight)

    /**
     * Updates the bird's physics for a single frame.
     *
     * Applies gravity to velocity, updates position, clamps position to the top of the screen,
     * and synchronizes the collision hitbox with the visual sprite.
     */
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

    /**
     * Applies an upward impulse to the bird, simulating a flap.
     */
    fun flap() {
        velocity = flapStrength
    }

    /**
     * Renders the bird sprite.
     *
     * @param batch The [SpriteBatch] used for drawing.
     * @param region The [TextureRegion] of the current animation frame to draw.
     */
    fun render(
            batch: com.badlogic.gdx.graphics.g2d.SpriteBatch,
            region: com.badlogic.gdx.graphics.g2d.TextureRegion
    ) {
        val rotation = (velocity * 2).coerceIn(-45f, 45f)
        batch.draw(region, x, y, width / 2, height / 2, width, height, 1f, 1f, rotation)
    }
}
