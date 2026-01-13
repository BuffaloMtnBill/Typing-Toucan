package com.tappybird.entities

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import kotlin.random.Random

class Pipe(var x: Float, val gapCenterY: Float, val isSnake: Boolean = false) {
    val headIndex = Random.nextInt(4)

    // Hitbox dimensions
    val width = 80f // Gameplay width (narrower than visual if transparent)

    // Visual dimensions
    // Asset is 512x2048 (AspectRatio 1:4 or 0.25 width/height)
    private val DRAW_WIDTH = 120f
    private val DRAW_HEIGHT = 480f // 120 * 4

    // Gap settings
    val gap = 220f

    // We want the GAP to be respected.
    // The obstacles are just visual tips.
    // The user said: "Use these graphics for the obstacles rather than dealing with tiling."
    // Does this mean the obstacle is ONLY 300px tall?
    // If the gap is high up, the bottom pipe needs to reach it.
    // Screen height 600. If gap is at 400..600. Bottom pipe top is at 400.
    // It needs to go down to 0. So 400px height.
    // If sprite is 300px, there is a gap at the bottom.
    // However, tiling is "off the table".
    // I will assume for now we just draw the sprite anchored at the gap edge.
    // If it floats, it floats (or maybe the user wants it to look like it's coming from offscreen).
    // For now: Draw sprite anchored at the gap.

    val minPipeHeight = 50f // Arbitrary min height near edges

    // Bottom Pipe Top = Center - Half Gap
    val bottomHeight = gapCenterY - gap / 2f

    // Top Pipe Bottom = Center + Half Gap
    val topY = gapCenterY + gap / 2f

    // Top Height = Arbitrary large value or we don't strictly need height for bounds unless we
    // clamp?
    // Let's assume ScreenHeight max is ~800, so extending up 1000 is safe.
    // Actually Logic: TopBounds starts at topY, extends UP.
    val topHeight = 2000f // Plenty of head room

    val bottomBounds = Rectangle(x, 0f, width, bottomHeight)
    val topBounds = Rectangle(x, topY, width, topHeight)

    var scored = false
    var collided = false

    fun update(delta: Float, speed: Float) {
        x -= speed * delta
        bottomBounds.setX(x)
        topBounds.setX(x)
        topBounds.setWidth(width)
        bottomBounds.setWidth(width)
    }

    fun render(batch: SpriteBatch, texture: Texture) {
        // Calculate centered draw X
        // Hitbox center: x + width/2
        // Draw center: drawX + DRAW_WIDTH/2
        // drawX = x + width/2 - DRAW_WIDTH/2
        val drawX = x + (width / 2) - (DRAW_WIDTH / 2)

        // --- BOTTOM PIPE ---
        // Top of sprite aligned with bottomHeight
        // Y = bottomHeight - DRAW_HEIGHT
        batch.draw(texture, drawX, bottomHeight - DRAW_HEIGHT, DRAW_WIDTH, DRAW_HEIGHT)

        // --- TOP PIPE ---
        // Bottom of sprite aligned with topY
        // Flipped vertically
        batch.draw(
                texture,
                drawX,
                topY,
                DRAW_WIDTH,
                DRAW_HEIGHT,
                0,
                0,
                texture.width,
                texture.height,
                false,
                true // Flip Y
        )
    }
}
