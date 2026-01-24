package com.typingtoucan.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Pool
import kotlin.random.Random

/**
 * Represents an obstacle pair (necks/snakes) that the bird must fly through.
 *
 * Manages the position, gap size, and rendering of the top and bottom obstacles.
 *
 * @property x The horizontal position of the obstacle.
 * @property gapCenterY The vertical center of the gap between the obstacles.
 * @property isSnake Whether this obstacle uses snake graphics instead of giraffe necks.
 */
class Neck() : Pool.Poolable {
    var x: Float = 0f
    var gapCenterY: Float = 0f
    var isSnake: Boolean = false

    /** Index for selecting random head variations (if applicable). */
    var headIndex = 0

    /** Width of the collision hitbox. */
    val width = 80f

    // Visual dimensions (Asset is 512x2048, aspect ratio 1:4)
    private val DRAW_WIDTH = 120f
    private val DRAW_HEIGHT = 480f

    /** The vertical space between the top and bottom obstacles. */
    val gap = 220f

    /** Minimum height for the neck to be visible. */
    val minNeckHeight = 50f

    /** Top Y-coordinate of the bottom obstacle. */
    val bottomHeight: Float
        get() = gapCenterY - gap / 2f

    /** Bottom Y-coordinate of the top obstacle. */
    val topY: Float
        get() = gapCenterY + gap / 2f

    /** Height of the top obstacle bounds. Uses a large value to extend off-screen. */
    val topHeight = 2000f

    /** Collision bounds for the bottom obstacle. */
    val bottomBounds = Rectangle(0f, 0f, width, 0f)
    
    /** Collision bounds for the top obstacle. */
    val topBounds = Rectangle(0f, 0f, width, topHeight)

    /** Tracks if this obstacle has been successfully passed by the player. */
    var scored = false
    
    /** Tracks if a collision has occurred with this obstacle. */
    var collided = false

    fun init(x: Float, gapCenterY: Float, isSnake: Boolean) {
        this.x = x
        this.gapCenterY = gapCenterY
        this.isSnake = isSnake
        this.headIndex = Random.nextInt(if (isSnake) 2 else 5)
        this.scored = false
        this.collided = false
        
        bottomBounds.set(x, 0f, width, bottomHeight)
        topBounds.set(x, topY, width, topHeight)
    }

    override fun reset() {
        x = 0f
        gapCenterY = 0f
        isSnake = false
        headIndex = 0
        scored = false
        collided = false
    }

    /**
     * Updates the horizontal position of the obstacle.
     *
     * @param delta Time elapsed since the last frame.
     * @param speed The speed at which the obstacle moves to the left.
     */
    fun update(delta: Float, speed: Float) {
        x -= speed * delta
        bottomBounds.setX(x)
        topBounds.setX(x)
        topBounds.setWidth(width)
        bottomBounds.setWidth(width)
    }

    /**
     * Renders the top and bottom obstacles.
     *
     * The sprites are drawn centered horizontally relative to the hitbox logic,
     * and anchored to the edges of the gap.
     *
     * @param batch The [SpriteBatch] used for drawing.
     * @param texture The texture to use for the obstacles.
     */
    fun render(batch: SpriteBatch, region: com.badlogic.gdx.graphics.g2d.TextureRegion) {
        // Calculate centered draw X
        val drawX = x + (width / 2) - (DRAW_WIDTH / 2)

        // --- BOTTOM NECK ---
        batch.draw(region, drawX, bottomHeight - DRAW_HEIGHT, DRAW_WIDTH, DRAW_HEIGHT)

        // --- TOP NECK ---
        // Flip the region temporarily to draw the top neck
        region.flip(false, true)
        batch.draw(region, drawX, topY, DRAW_WIDTH, DRAW_HEIGHT)
        region.flip(false, true) // Flip back
    }
}
