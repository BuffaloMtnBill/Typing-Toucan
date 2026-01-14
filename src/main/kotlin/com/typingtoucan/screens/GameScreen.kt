package com.typingtoucan.screens

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.typingtoucan.TypingToucanGame
import com.typingtoucan.entities.Bird
import com.typingtoucan.entities.Neck
import com.typingtoucan.systems.DifficultyManager
import com.typingtoucan.utils.SaveManager

class GameScreen(
        private val game: TypingToucanGame,
        var difficulty: DifficultyManager.Difficulty = DifficultyManager.Difficulty.NORMAL,
        private val isPracticeMode: Boolean = false,
        private val customSource: com.typingtoucan.systems.TypingSource? = null
) : Screen, InputProcessor {
    private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 600f) }
    private val viewport = com.badlogic.gdx.utils.viewport.ExtendViewport(800f, 600f, camera)

    // Font assets
    private var uiFont: com.badlogic.gdx.graphics.g2d.BitmapFont
    private var queueFont: com.badlogic.gdx.graphics.g2d.BitmapFont
    private val layout = GlyphLayout()
    private var smallFont: com.badlogic.gdx.graphics.g2d.BitmapFont

    // Graphics assets
    private lateinit var backgroundTextures: List<Texture>
    private lateinit var groundTexture: Texture
    private lateinit var victoryTexture: Texture
    private lateinit var monkeyTextures: List<Texture>

    private lateinit var currentMonkeyTexture: Texture

    // Ground Animation
    private lateinit var groundAnimTextures: Array<Texture>
    private lateinit var groundAnimation: Animation<TextureRegion>
    private var groundState = GroundState.IDLE
    private var groundAnimTimer = 0f
    private var timeUntilNextAnim = 5f // Start with 5s delay
    private var groundStateTime = 0f

    private enum class GroundState {
        IDLE,
        ANIMATING
    }

    // Single Sprite Obstacles (50x300)
    private lateinit var giraffeObstacles: Array<Texture>
    private lateinit var anacondaObstacles: Array<Texture>

    // Toucan Assets
    private lateinit var toucanFrames: Array<Texture>
    private lateinit var toucanPainTexture: Texture

    // Animation
    private val birdAnimation: Animation<TextureRegion>
    private var stateTime = 0f

    // Game State
    private val shapeRenderer = ShapeRenderer()
    private val bird = Bird(90f, 400f)
    private val necks = com.badlogic.gdx.utils.Array<Neck>()
    private val typingQueue =
            com.typingtoucan.systems.TypingQueue(
                    customSource ?: com.typingtoucan.systems.ClassicSource()
            )
    private var diffManager = DifficultyManager(difficulty)
    private enum class PauseState {
        NONE,
        MAIN,
        AUDIO,
        VICTORY
    }
    private var pauseState: PauseState = PauseState.NONE

    private val mainMenuItems =
            if (customSource is com.typingtoucan.systems.CustomPoolSource) {
                listOf("Resume", "Difficulty", "Audio", "Letter Selection Menu", "Main Menu")
            } else if (customSource is com.typingtoucan.systems.TextSnippetSource) {
                listOf("Resume", "Difficulty", "Audio", "Select Text", "Main Menu")
            } else {
                listOf("Resume", "Difficulty", "Capitals", "Audio", "Main Menu")
            }
    private val audioMenuItems = listOf("Sound", "Music", "Music Track", "Back")
    private var menuSelectedIndex = 0
    private val soundManager = game.soundManager

    private var neckTimer = 0f
    private var nextNeckInterval = diffManager.neckInterval // Initialize with default
    private var score = 0
    private var progressionPoints = 0
    private var displayProgression = 0f // Fluid animation value
    // private var gameOver = false // Removed for robustness
    private var gameStarted = false
    private var hurtTimer = 0f // Timer for red flash effect
    private var flashTimer = 0f // Timer for green "level up" flash
    private var justUnlockedChar = ""
    private var milestoneTimer = 0f // Timer for milestone "pulse" effect
    private var level = 1
    private var totalNecksSpawned = 0

    // Weight Flash
    private var weightFlashValue = 0
    private var weightFlashTimer = 0f

    // Scrolling
    private var backgroundX = 0f
    private var groundX = 0f
    private var currentBgIndex = 0
    private var nextBgIndex = 0 // Only one background now

    // Practice Mode Stats
    private var streak = 0
    private var maxStreak = 0

    // Monkey Decoration
    private var monkeyX = 500f

    // Cached Draw Strings
    private var cachedQueueStr = ""
    private var cachedLevelStr = "1"
    private var cachedWeightStr = ""
    private val descenders = listOf("g", "j", "p", "q", "y") // Static allocation

    // Performance Caches
    private val tempColor = Color() // Reusable color object
    private lateinit var toucanPainRegion: TextureRegion
    private val practiceModeLayout = GlyphLayout()
    private val startTextLayout = GlyphLayout()
    private val escTextLayout = GlyphLayout()
    private val victoryTextLayout = GlyphLayout()
    private val victorySubTextLayout = GlyphLayout()

    init {
        Gdx.input.inputProcessor = this

        // Initialize Font
        val generator =
                FreeTypeFontGenerator(Gdx.files.internal("assets/OriginalSurfer-Regular.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 20
        parameter.color = Color.WHITE
        parameter.borderColor = Color.BLACK
        parameter.borderWidth = 2f
        uiFont = generator.generateFont(parameter)

        // Create large font for queue display
        val queueParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        queueParam.size = 60 // Large size
        queueParam.color = Color.WHITE // Will be updated dynamically
        queueParam.borderColor = Color.BLACK
        queueParam.borderWidth = 4f // Thicker border
        // Shadow removed
        queueFont = generator.generateFont(queueParam)

        // Create smaller font for "LEVEL" label
        val labelParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        labelParam.size = 14
        labelParam.color = Color.WHITE
        labelParam.borderColor = Color.BLACK
        labelParam.borderWidth = 1f
        smallFont = generator.generateFont(labelParam)

        generator.dispose()

        // Set Texture Filters
        // Retrieve Assets from Manager
        backgroundTextures = listOf(game.assetManager.get("assets/background_panoramic.png"))

        groundTexture = game.assetManager.get("assets/ground.png")
        victoryTexture = game.assetManager.get("assets/victory_background.png")

        // Load Ground Animation Textures (Manually for now since not in AssetManager yet?
        // Actually, we should probably add them to AssetManager or load direct if safe.
        // For robustness in this context, let's assume they are available via Gdx.files if not
        // managed,
        // but better to use Texture(Gdx.files...) if they aren't preloaded.
        // Since we just created them, they aren't in TappyBirdGame's loading list.
        // so we load them directly.
        groundAnimTextures =
                Array(2) { i ->
                    Texture(Gdx.files.internal("assets/ground_anim_${i + 1}.png")).apply {
                        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                        setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
                    }
                }

        val frames = com.badlogic.gdx.utils.Array<TextureRegion>()
        // Pattern: Main -> Anim1 -> Anim2 -> Anim1 -> Loop?
        // User said: "three frames". Main, Anim1, Anim2.
        // "cycle between two other frames"
        // Let's just cycle anim1, anim2.
        frames.add(TextureRegion(groundAnimTextures[0]))
        frames.add(TextureRegion(groundAnimTextures[1]))
        groundAnimation = Animation(0.1f, frames, Animation.PlayMode.LOOP)

        monkeyTextures =
                listOf(
                        game.assetManager.get("assets/banana_monkey_1.png"),
                        game.assetManager.get("assets/banana_monkey_2.png"),
                        game.assetManager.get("assets/banana_monkey_3.png"),
                        game.assetManager.get("assets/young_monkey_1.png"),
                        game.assetManager.get("assets/young_monkey_2.png"),
                        game.assetManager.get("assets/young_monkey_3.png"),
                        game.assetManager.get("assets/old_monkey_1.png"),
                        game.assetManager.get("assets/old_monkey_2.png"),
                        game.assetManager.get("assets/old_monkey_3.png")
                )
        currentMonkeyTexture = monkeyTextures.random()

        giraffeObstacles = Array(5) { i -> game.assetManager.get("assets/giraffe${i + 1}.png") }
        anacondaObstacles = Array(2) { i -> game.assetManager.get("assets/anaconda_long_$i.png") }
        toucanFrames = Array(4) { i -> game.assetManager.get("assets/toucan_$i.png") }
        toucanPainTexture = game.assetManager.get("assets/toucan_pain.png")

        // Set Texture Filters
        backgroundTextures.forEach {
            it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        monkeyTextures.forEach {
            it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        currentMonkeyTexture = monkeyTextures.random()

        groundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        giraffeObstacles.forEach {
            it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        anacondaObstacles.forEach {
            it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        toucanFrames.forEach {
            it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
        toucanPainTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        // Setup Bird Animation
        val birdRegions = com.badlogic.gdx.utils.Array<TextureRegion>(4)
        toucanFrames.forEach { texture -> birdRegions.add(TextureRegion(texture)) }
        birdAnimation = Animation(0.1f, birdRegions, Animation.PlayMode.LOOP)

        // Setup textures for tiling/wrapping
        groundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        // No need to wrap obstacles anymore as we aren't tiling

        updateQueueString()

        // Apply initial difficulty gravity and flap strength
        bird.gravity = diffManager.gravity
        bird.flapStrength = diffManager.flapStrength

        // Initial Monkey Pos for first run
        val scale = 150f / currentMonkeyTexture.height.toFloat()
        val mWidth = currentMonkeyTexture.width * scale
        val distanceToSpawn = diffManager.scrollSpeed * nextNeckInterval
        monkeyX = viewport.worldWidth + distanceToSpawn - 90f - mWidth

        // Initialize Performance Caches
        toucanPainRegion = TextureRegion(toucanPainTexture)
        practiceModeLayout.setText(uiFont, "PRACTICE MODE")
        startTextLayout.setText(queueFont, "TYPE TO START")
        escTextLayout.setText(uiFont, "Press ESC for Menu")
        victoryTextLayout.setText(queueFont, "VICTORY!")
        victorySubTextLayout.setText(uiFont, "Press ENTER to Return to Menu")

        // Sync Capitals Preference
        typingQueue.setCapitalsEnabled(com.typingtoucan.utils.SaveManager.loadCapitalsEnabled())
    }

    private fun updateQueueString() {
        cachedQueueStr = typingQueue.queue.joinToString("   ")
    }

    override fun show() {
        Gdx.input.inputProcessor = this
        updateQueueString()

        // Switch to Game Music but don't play yet (wait for gameStarted)
        game.soundManager.currentTrack = com.typingtoucan.systems.SoundManager.MusicTrack.WHAT
        game.soundManager.stopMusic()
    }

    private fun endGame() {
        try {
            soundManager.playCrash()
            // Penalize queue on crash?
            if (typingQueue.queue.isNotEmpty()) {
                val activeChar = typingQueue.queue.first()
                typingQueue.onCrash(activeChar)
            }

            if (!isPracticeMode) {
                SaveManager.saveHighScore(score)
            }
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Error during endGame", e)
        } finally {
            softReset()
        }
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (pauseState == PauseState.VICTORY) {
                game.screen = MenuScreen(game)
            } else if (pauseState == PauseState.NONE) {
                pauseState = PauseState.MAIN
                menuSelectedIndex = 0
            } else if (pauseState == PauseState.AUDIO) {
                pauseState = PauseState.MAIN
                menuSelectedIndex = 3
            } else {
                pauseState = PauseState.NONE
            }
        }

        // Handle Enter for Victory
        if (pauseState == PauseState.VICTORY &&
                        (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                                Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
        ) {
            game.screen = MenuScreen(game)
        }

        if (gameStarted && pauseState == PauseState.NONE) {
            update(delta)
        } else {
            // If paused or ready state, animate bird in place?
            if (pauseState == PauseState.NONE) {
                // Ready state (Type to Start)
                stateTime += delta
                bird.y = 300f + kotlin.math.sin(stateTime * 5f) * 10f
                bird.velocity = 0f
            } else {
                handlePauseMenuInput()
            }
        }

        draw(delta)
    }

    private fun update(delta: Float) {
        bird.update()
        stateTime += delta

        // Update hurt timer
        if (hurtTimer > 0) {
            hurtTimer -= delta
            if (hurtTimer < 0) hurtTimer = 0f
        }

        // Ground Animation Logic
        if (groundState == GroundState.IDLE) {
            timeUntilNextAnim -= delta
            if (timeUntilNextAnim <= 0) {
                groundState = GroundState.ANIMATING
                // Random duration 0.5 - 1.5s
                groundAnimTimer = 0.5f + Math.random().toFloat() * 1.0f
            }
        } else {
            groundAnimTimer -= delta
            groundStateTime += delta
            if (groundAnimTimer <= 0) {
                groundState = GroundState.IDLE
                groundStateTime = 0f
                // Random next interval 3s - 8s
                timeUntilNextAnim = 3f + Math.random().toFloat() * 5f
            }
        }
        if (flashTimer > 0) {
            flashTimer -= delta
            if (flashTimer < 0) flashTimer = 0f
        }
        if (milestoneTimer > 0) {
            milestoneTimer -= delta
            if (milestoneTimer < 0) milestoneTimer = 0f
        }

        // Fluid Process Bar Update
        val targetProgression = if (flashTimer > 0) 5f else progressionPoints.toFloat()
        // Speed 5.0f gives a nice snappy but smooth feel
        displayProgression += (targetProgression - displayProgression) * 5f * delta

        // Scroll background/ground
        backgroundX -= 10f * delta
        groundX -= diffManager.scrollSpeed * delta
        monkeyX -= diffManager.scrollSpeed * delta

        val currentBg = backgroundTextures[currentBgIndex]
        val scaleFactor = viewport.worldHeight / currentBg.height.toFloat()
        val renderedWidth = currentBg.width * scaleFactor

        if (backgroundX <= -renderedWidth) {
            backgroundX += renderedWidth
            currentBgIndex = nextBgIndex
            nextBgIndex = backgroundTextures.indices.random()
        }
        if (groundX <= -groundTexture.width.toFloat()) groundX = 0f
        // Neck Spawning
        neckTimer += delta
        if (neckTimer >= nextNeckInterval) {
            neckTimer = 0f
            // Calculate next interval with variance
            val base = diffManager.neckInterval
            // Random variance between 0.7 (70%) and 1.3 (130%)
            val variance = 0.7f + Math.random().toFloat() * 0.6f
            nextNeckInterval = base * variance

            // Spawn Neck offscreen
            // CenterGap is randomly placed
            // Height is 600... we want gap roughly in middle area
            val minGapY = 150f
            val maxGapY = viewport.worldHeight - 150f
            val gapY = (minGapY.toInt()..maxGapY.toInt()).random().toFloat()

            // Pass the screen height to neck if needed, or just neck handles it?
            // Neck is just x,y,gap,width.
            // X should be worldWidth + some buffer
            val isSnake = Math.random() < 0.125 // 1/8 chance
            necks.add(Neck(viewport.worldWidth + 50f, gapY, isSnake))
            totalNecksSpawned++
        }

        val iterator = necks.iterator()
        while (iterator.hasNext()) {
            val neck = iterator.next()
            neck.update(delta, diffManager.scrollSpeed)

            if (neck.x + neck.width < 0) {
                iterator.remove()
            }

            if (!neck.scored && neck.x < bird.x) {
                neck.scored = true
                score++
                soundManager.playScore()
                soundManager.playScore()
                progressionPoints++
                if (progressionPoints >= 5) {
                    // Check for Victory
                    if (typingQueue.isFullyUnlocked()) {
                        // VICTORY!
                        pauseState = PauseState.VICTORY
                        soundManager.playLevelUp() // Or victory sound if we had one
                        // Stop movement handled by pauseState check in update/keyTyped
                    } else {
                        val unlockedChars = typingQueue.expandPool()
                        if (unlockedChars.isNotEmpty()) {
                            level++
                            cachedLevelStr = level.toString() // Cache Update
                            flashTimer = 1.0f // Flash green for 1.0 second

                            // Handle display text
                            if (unlockedChars.size > 1) {
                                // Bulk unlock (e.g. Capitals)
                                val first = unlockedChars.first()
                                val last = unlockedChars.last()
                                justUnlockedChar = "$first-$last"
                            } else {
                                // Single unlock
                                justUnlockedChar = unlockedChars.first().toString()
                            }

                            soundManager.playLevelUp()

                            // Check for milestone
                            if (level % 5 == 0) {
                                milestoneTimer = 2.0f // Pulse for 2 seconds (starts loop)
                            }
                        }
                    }
                    progressionPoints = 0
                }
            }

            if (!neck.collided &&
                            (Intersector.overlaps(bird.bounds, neck.bottomBounds) ||
                                    Intersector.overlaps(bird.bounds, neck.topBounds))
            ) {
                if (!isPracticeMode) {
                    // Neck collision is now non-fatal
                    neck.collided = true
                    soundManager.playCrash()
                    hurtTimer = 0.5f // Flash red and show pain sprite

                    // RECOIL: Bounce bird up slightly to prevent "dead drop"
                    bird.velocity = 7f // A bit less than a full flap (10f)

                    // Penalty?
                    if (typingQueue.queue.isNotEmpty()) {
                        progressionPoints = (progressionPoints - 1).coerceAtLeast(0)
                        // Maybe play error sound too? Or crash is enough? Crash is enough.
                    }
                }
            }
        }

        if (bird.bounds.y <= 60) { // Screen height for ground is 60
            if (isPracticeMode) {
                streak = 0
                bird.y = 80f // Reset slightly above ground
                bird.velocity = 0f
                bird.flap()
                soundManager.playFlap()
            } else {
                endGame()
            }
        }
    }

    private fun softReset() {
        gameStarted = false
        progressionPoints = 0
        score = 0
        if (score > 0) {
            // Only reset score? Or keep score? usually score resets.
            // User only asked for Level.
        }
        score = 0
        // level = 1  <-- Removed to persist level
        // cachedLevelStr = level.toString() // If we reset level we would update here.

        // Reset Streak logic
        if (isPracticeMode) {
            streak = 0
            // maxStreak persists as long as screen exists
        }

        bird.x = 100f
        bird.y = 300f
        bird.velocity = 0f
        necks.clear()
        hurtTimer = 0f
        flashTimer = 0f

        // Reset Neck Logic for determinstic spawn
        neckTimer = 0f
        nextNeckInterval = diffManager.neckInterval

        // Position Monkey: Screen End + Distance to spawn - 90 buffer - MonkeyWidth
        currentMonkeyTexture = monkeyTextures.random()
        val scale = 150f / currentMonkeyTexture.height.toFloat()
        val mWidth = currentMonkeyTexture.width * scale
        val distanceToSpawn = diffManager.scrollSpeed * nextNeckInterval
        monkeyX = viewport.worldWidth + distanceToSpawn - 90f - mWidth

        updateQueueString() // Ensure queue is visualy sync
    }

    private fun draw(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.3f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        game.batch.projectionMatrix = camera.combined
        shapeRenderer.projectionMatrix = camera.combined

        // --- BATCH PHASE 1: WORLD ---
        game.batch.begin()

        // Draw Background (Tiled to cover viewport)
        // Ensure we cover worldWidth. Texture is likely 800.
        // We draw two instances usually to loop. If world is > 800, we might need more?
        // Basic loop logic: draw at backgroundX, backgroundX+800.
        // If worldWidth > 800, we need another one at +1600 etc?
        // Simple fix: Draw enough to cover viewport.
        // For simplicity in this iteration, assuming worldWidth < 1600 usually.
        // But let's be robust.
        // Draw Background (Variable Width Support)
        // Draw Background (Variable Width Support + Proportional Scaling)
        val currentBg = backgroundTextures[currentBgIndex]
        // Scale width to match the vertical scaling (viewportHeight / textureHeight)
        val scaleFactor = viewport.worldHeight / currentBg.height.toFloat()
        val renderedWidth = currentBg.width * scaleFactor

        // Draw Current
        game.batch.draw(currentBg, backgroundX, 0f, renderedWidth, viewport.worldHeight)

        // Draw Next (if needed to fill screen)
        val rightEdge = backgroundX + renderedWidth
        if (rightEdge < viewport.worldWidth) {
            val nextBg = backgroundTextures[nextBgIndex]
            val nextScaleFactor = viewport.worldHeight / nextBg.height.toFloat()
            val nextRenderedWidth = nextBg.width * nextScaleFactor

            game.batch.draw(nextBg, rightEdge, 0f, nextRenderedWidth, viewport.worldHeight)

            // Handle edge case: very wide screen vs very narrow backgrounds
            // If screen is wider than current + next, we might need more?
            // For now assuming 2 images cover the screen (800+800 >= 1600 is usually enough for
            // mobile/desktop)
            // If we have very wide screens or tiny backgrounds, we'd need a loop here.
            // Given assets are likely >= 800, this simple double-draw is sufficient.
        }

        // Draw Necks with Heads
        for (neck in necks) {
            val texture =
                    if (neck.isSnake) {
                        anacondaObstacles[neck.headIndex % 2]
                    } else {
                        giraffeObstacles[neck.headIndex]
                    }
            neck.render(game.batch, texture)
        }

        // Draw Ground (Tiled)
        // Draw Ground (Dynamic Tiling)
        val groundW = groundTexture.width.toFloat()
        val numGroundTiles = kotlin.math.ceil(viewport.worldWidth / groundW).toInt() + 1

        val textureToDraw =
                if (groundState == GroundState.ANIMATING) {
                    groundAnimation.getKeyFrame(groundStateTime).texture
                } else {
                    groundTexture
                }

        for (i in 0 until numGroundTiles) {
            game.batch.draw(textureToDraw, groundX + i * groundW, 0f, groundW, 70f)
        }

        // Draw Monkey (if visible)
        if (monkeyX > -200) { // Simple culling
            val scale = 150f / currentMonkeyTexture.height.toFloat()
            val width = currentMonkeyTexture.width * scale
            game.batch.draw(currentMonkeyTexture, monkeyX, 40f, width, 150f)
        }

        // Draw Bird
        val currentFrame =
                if (hurtTimer > 0) {
                    toucanPainRegion // Use cached region
                } else {
                    birdAnimation.getKeyFrame(stateTime, true)
                }
        bird.render(game.batch, currentFrame)

        game.batch.end()

        // --- SHAPE PHASE ---
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // 1. Hurt Flash
        if (hurtTimer > 0) {
            shapeRenderer.color = Color(1f, 0f, 0f, 0.5f * (hurtTimer / 0.3f))
            shapeRenderer.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        }

        if (!isPracticeMode) {
            // 2. Status Bar
            var barWidth = 200f
            var barHeight = 20f
            var barX = viewport.worldWidth / 2f - barWidth / 2f
            var barY = viewport.worldHeight - 50f
            var fillColor = Color.CYAN

            if (flashTimer > 0) {
                val scale =
                        1.0f +
                                0.6f *
                                        kotlin.math.sin(flashTimer * 10f).let {
                                            if (it < 0) -it else it
                                        }
                barWidth *= scale
                barHeight *= scale
                barX = viewport.worldWidth / 2f - barWidth / 2f
                barY = viewport.worldHeight - 50f - barHeight / 2f

                if ((flashTimer * 10).toInt() % 2 == 0) {
                    fillColor = Color.GREEN
                } else {
                    fillColor = Color.GOLD
                }
            }

            val progressRatio = displayProgression / 5f
            // Use tempColor to avoid allocation
            tempColor.set(Color.DARK_GRAY).lerp(Color.GOLD, progressRatio)
            shapeRenderer.color = tempColor
            val borderThickness = 4f
            shapeRenderer.rect(
                    barX - borderThickness,
                    barY - borderThickness,
                    barWidth + borderThickness * 2,
                    barHeight + borderThickness * 2
            )

            if (flashTimer > 0) {
                shapeRenderer.color = fillColor
                shapeRenderer.rect(barX, barY, barWidth, barHeight)
            } else {
                shapeRenderer.color = Color.DARK_GRAY
                shapeRenderer.rect(barX, barY, barWidth, barHeight)
                shapeRenderer.color = fillColor
                val fillWidth = (displayProgression / 5f) * barWidth
                shapeRenderer.rect(barX, barY, fillWidth, barHeight)
            }
        }

        // 3. Pause Menu Overlay BG
        if (pauseState != PauseState.NONE) {
            shapeRenderer.color = Color(0f, 0f, 0f, 0.7f)
            shapeRenderer.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        }

        // 4. Underscore (If applicable)
        if (pauseState == PauseState.NONE && typingQueue.queue.isNotEmpty()) {
            layout.setText(queueFont, cachedQueueStr)
            val textX = viewport.worldWidth / 2f - layout.width / 2
            val textY = viewport.worldHeight / 2f + viewport.worldHeight * 0.1f

            val firstCharStr = typingQueue.queue.first().toString()
            val firstCharLayout = GlyphLayout(queueFont, firstCharStr)

            // Pulsate alpha
            val alpha = 0.5f + 0.5f * kotlin.math.sin(stateTime * 10f)
            shapeRenderer.color = Color(1f, 0f, 0f, alpha)

            val underscoreWidth = 40f
            val centerX = textX + firstCharLayout.width / 2f
            val rectX = centerX - underscoreWidth / 2f
            val yOffset =
                    if (descenders.any { firstCharStr.contains(it, ignoreCase = true) }) 70f
                    else 55f
            shapeRenderer.rect(rectX, textY - yOffset, underscoreWidth, 5f)
        }

        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        // --- BATCH PHASE 2: UI & TEXT ---
        game.batch.begin()

        if (isPracticeMode) {
            // Draw "Practice Mode"
            uiFont.draw(
                    game.batch,
                    practiceModeLayout,
                    viewport.worldWidth / 2f - practiceModeLayout.width / 2,
                    viewport.worldHeight - 20f
            )
            // Draw "ESC for menu"
            val escLabel = "ESC for menu"
            layout.setText(smallFont, escLabel)
            smallFont.draw(
                    game.batch,
                    escLabel,
                    viewport.worldWidth / 2f - layout.width / 2,
                    viewport.worldHeight - 50f
            )
        }

        // Level / Streak Indicator
        if (isPracticeMode) {
            val levelCommonCenter = viewport.worldWidth - 60f

            // Draw Max Streak (Larger)
            val maxLabel = "MAX STREAK"
            layout.setText(smallFont, maxLabel)
            smallFont.draw(game.batch, maxLabel, levelCommonCenter - layout.width / 2, 170f)

            // User requested reduction by 50%. queueFont is 60. 0.5 scale = 30.
            val maxVal = maxStreak.toString()
            queueFont.data.setScale(0.5f)
            layout.setText(queueFont, maxVal)
            queueFont.draw(
                    game.batch,
                    maxVal,
                    levelCommonCenter - layout.width / 2,
                    135f
            ) // Adjusted Y slightly for smaller font
            queueFont.data.setScale(1.0f) // Reset

            // Draw Current Streak
            val streakLabel = "STREAK"
            layout.setText(smallFont, streakLabel)
            smallFont.draw(game.batch, streakLabel, levelCommonCenter - layout.width / 2, 90f)

            // Pulse Effect for Streak Value (using milestoneTimer)
            if (milestoneTimer > 0) {
                val pulse = kotlin.math.abs(kotlin.math.sin(stateTime * 5f))
                val scale = 1.0f + 0.4f * pulse
                queueFont.data.setScale(scale)
                val colorPulse = (kotlin.math.sin(stateTime * 5f) + 1f) / 2f

                // Avoid allocation
                tempColor.set(Color.GOLD).lerp(Color.CYAN, colorPulse)
                queueFont.color = tempColor
            }

            val streakVal = streak.toString()
            layout.setText(queueFont, streakVal)
            queueFont.draw(game.batch, streakVal, levelCommonCenter - layout.width / 2, 60f)

            // Reset Font State
            queueFont.data.setScale(1.0f)
            queueFont.color = Color.WHITE
        } else {
            val levelLabel = "LEVEL"
            layout.setText(smallFont, levelLabel)
            val levelLabelWidth = layout.width
            val levelCommonCenter = viewport.worldWidth - 60f

            smallFont.draw(game.batch, levelLabel, levelCommonCenter - levelLabelWidth / 2, 100f)

            // Level Value
            if (milestoneTimer > 0) {
                val pulse = kotlin.math.abs(kotlin.math.sin(stateTime * 5f))
                val scale = 1.0f + 0.4f * pulse
                queueFont.data.setScale(scale)
                val colorPulse = (kotlin.math.sin(stateTime * 5f) + 1f) / 2f

                // Avoid allocation
                tempColor.set(Color.GOLD).lerp(Color.CYAN, colorPulse)
                queueFont.color = tempColor
            }

            if (cachedLevelStr != null) {
                layout.setText(queueFont, cachedLevelStr) // Use cached
                val valWidth = layout.width
                queueFont.draw(game.batch, cachedLevelStr, levelCommonCenter - valWidth / 2, 70f)
            }
        }
        // RESET FONT STATE
        queueFont.data.setScale(1.0f)
        queueFont.color = Color.WHITE

        // New Letter Flash
        if (flashTimer > 0) {
            layout.setText(queueFont, justUnlockedChar)
            val pulse = kotlin.math.abs(kotlin.math.sin(flashTimer * 10f))
            val scale = 1.0f + 0.5f * pulse
            queueFont.data.setScale(scale)
            queueFont.draw(
                    game.batch,
                    justUnlockedChar,
                    viewport.worldWidth / 2f - layout.width / 2,
                    viewport.worldHeight - 80f
            )
            queueFont.data.setScale(1.0f)
        }

        // Pause Menu Logic
        if (pauseState != PauseState.NONE) {
            val menuTitle = "PAUSED"
            layout.setText(uiFont, menuTitle)
            uiFont.draw(
                    game.batch,
                    menuTitle,
                    viewport.worldWidth / 2f - layout.width / 2,
                    viewport.worldHeight - 100f
            )

            if (pauseState == PauseState.MAIN) drawPauseMainMenu()
            else if (pauseState == PauseState.AUDIO) drawAudioMenu()
        } else if (!gameStarted) {
            queueFont.draw(
                    game.batch,
                    startTextLayout,
                    viewport.worldWidth / 2f - startTextLayout.width / 2,
                    viewport.worldHeight * 0.76f
            )
            uiFont.draw(
                    game.batch,
                    escTextLayout,
                    viewport.worldWidth / 2f - escTextLayout.width / 2,
                    30f
            )
        }

        // Queue Text
        if (pauseState == PauseState.NONE) {
            // Underscore already drawn in shapes
            layout.setText(queueFont, cachedQueueStr)
            val textX = viewport.worldWidth / 2f - layout.width / 2
            val textY = viewport.worldHeight / 2f + viewport.worldHeight * 0.1f
            queueFont.draw(game.batch, cachedQueueStr, textX, textY)
        }

        // Draw Victory Screen
        if (pauseState == PauseState.VICTORY) {
            game.batch.draw(victoryTexture, 0f, 0f, viewport.worldWidth, viewport.worldHeight)

            game.batch.draw(victoryTexture, 0f, 0f, viewport.worldWidth, viewport.worldHeight)

            queueFont.draw(
                    game.batch,
                    victoryTextLayout,
                    viewport.worldWidth / 2f - victoryTextLayout.width / 2,
                    viewport.worldHeight - 100f
            )

            uiFont.draw(
                    game.batch,
                    victorySubTextLayout,
                    viewport.worldWidth / 2f - victorySubTextLayout.width / 2,
                    50f
            )
        }

        game.batch.end()
    }

    override fun keyDown(keycode: Int): Boolean = false
    override fun keyUp(keycode: Int): Boolean = false
    override fun keyTyped(character: Char): Boolean {
        // if (!gameOver) { // Removed check
        if (pauseState != PauseState.NONE) return false
        val result = typingQueue.handleInput(character, updateWeights = !isPracticeMode)
        if (result != null) {
            if (!gameStarted) {
                gameStarted = true
                soundManager.playMusic()
            }

            // Update Cache
            updateQueueString()
            weightFlashValue = result
            cachedWeightStr = weightFlashValue.toString()

            weightFlashTimer = 1.0f
            bird.flap()
            soundManager.playFlap()

            if (isPracticeMode) {
                streak++
                if (streak > maxStreak) maxStreak = streak

                // Flash every 10 streaks
                if (streak % 10 == 0) {
                    soundManager.playLevelUpPractice()
                    milestoneTimer = 2.0f // Pulse for 2 seconds
                }
            }

            return true
        } else {
            // Wrong Key Logic
            if (gameStarted && typingQueue.queue.isNotEmpty()) {
                // Visual penalty
                hurtTimer = 0.3f
                // Audio penalty
                soundManager.playError()
                // Progression penalty
                progressionPoints = (progressionPoints - 1).coerceAtLeast(0)

                if (isPracticeMode) {
                    streak = 0
                }
            }
        }
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
    override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false
    override fun scrolled(amountX: Float, amountY: Float): Boolean = false
    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
            false

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true) // True centers camera
        camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)
    }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        uiFont.dispose()
        smallFont.dispose()
        queueFont.dispose()
        // generator.dispose() // Generator is disposed in init now
        shapeRenderer.dispose()

        // Textures needed to be disposed manually before AssetManager.
        // Now AssetManager owns them. We do NOT dispose them here.
        // TappyBirdGame disposes AssetManager on exit.

        // soundManager is owned by Game, do not dispose here

        // Dispose locally loaded textures
        if (::groundAnimTextures.isInitialized) {
            groundAnimTextures.forEach { it.dispose() }
        }
    }

    private fun drawPauseMainMenu() {
        var startY = viewport.worldHeight / 2f
        val gap = 50f
        val centerX = viewport.worldWidth / 2f

        mainMenuItems.forEachIndexed { index, item ->
            val label =
                    when (item) {
                        "Difficulty" -> "Difficulty: ${difficulty.name}"
                        "Capitals" ->
                                "Capitals: ${if (com.typingtoucan.utils.SaveManager.loadCapitalsEnabled()) "ON" else "OFF"}"
                        else -> item
                    }
            val isSelected = index == menuSelectedIndex
            uiFont.color = if (isSelected) Color.CYAN else Color.WHITE

            layout.setText(uiFont, label)
            val x = centerX - layout.width / 2
            val y = startY - (index * gap)
            uiFont.draw(game.batch, label, x, y)
        }
        uiFont.color = Color.WHITE
    }

    private fun drawAudioMenu() {
        var startY = viewport.worldHeight / 2f
        val gap = 50f
        val centerX = viewport.worldWidth / 2f

        audioMenuItems.forEachIndexed { index, item ->
            val label =
                    when (item) {
                        "Sound" -> "Sound: ${if (game.soundManager.soundEnabled) "ON" else "OFF"}"
                        "Music" -> "Music: ${if (game.soundManager.musicEnabled) "ON" else "OFF"}"
                        "Music Track" -> {
                            val trackName =
                                    if (game.soundManager.currentTrack ==
                                                    com.typingtoucan.systems.SoundManager.MusicTrack
                                                            .WHAT
                                    )
                                            "What"
                                    else "Dark Forest"
                            "Music Track: $trackName"
                        }
                        else -> item
                    }

            val isSelected = index == menuSelectedIndex
            uiFont.color = if (isSelected) Color.CYAN else Color.WHITE

            layout.setText(uiFont, label)
            uiFont.draw(game.batch, label, centerX - layout.width / 2, startY - (index * gap))
        }
        uiFont.color = Color.WHITE
    }

    private fun handlePauseMenuInput() {
        if (pauseState == PauseState.NONE) return

        val currentListSize =
                if (pauseState == PauseState.MAIN) mainMenuItems.size else audioMenuItems.size

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            menuSelectedIndex--
            if (menuSelectedIndex < 0) menuSelectedIndex = currentListSize - 1
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            menuSelectedIndex++
            if (menuSelectedIndex >= currentListSize) menuSelectedIndex = 0
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
        ) {
            if (pauseState == PauseState.MAIN) {
                val selectedItem = mainMenuItems[menuSelectedIndex]
                if (selectedItem == "Resume") {
                    pauseState = PauseState.NONE
                } else if (selectedItem == "Difficulty") {
                    difficulty =
                            when (difficulty) {
                                DifficultyManager.Difficulty.EASY ->
                                        DifficultyManager.Difficulty.NORMAL
                                DifficultyManager.Difficulty.NORMAL ->
                                        DifficultyManager.Difficulty.HARD
                                DifficultyManager.Difficulty.HARD ->
                                        DifficultyManager.Difficulty.INSANE
                                DifficultyManager.Difficulty.INSANE ->
                                        DifficultyManager.Difficulty.EASY
                            }
                    diffManager = DifficultyManager(difficulty)
                    bird.gravity = diffManager.gravity
                    bird.flapStrength = diffManager.flapStrength
                    nextNeckInterval = diffManager.neckInterval
                } else if (selectedItem == "Capitals") {
                    val current = com.typingtoucan.utils.SaveManager.loadCapitalsEnabled()
                    val newState = !current
                    com.typingtoucan.utils.SaveManager.saveCapitalsEnabled(newState)
                    typingQueue.setCapitalsEnabled(newState)
                } else if (selectedItem == "Letter Selection Menu") {
                    game.screen = CustomSetupScreen(game)
                } else if (selectedItem == "Select Text") {
                    game.screen = TextSetupScreen(game)
                } else if (selectedItem == "Audio") {
                    pauseState = PauseState.AUDIO
                    menuSelectedIndex = 0
                } else if (selectedItem == "Main Menu") {
                    game.soundManager.stopMusic()
                    game.screen = MenuScreen(game)
                }
            } else if (pauseState == PauseState.AUDIO) {
                val sm = game.soundManager
                when (menuSelectedIndex) {
                    0 -> sm.soundEnabled = !sm.soundEnabled // Toggle Sound
                    1 -> sm.musicEnabled = !sm.musicEnabled // Toggle Music
                    2 -> { // Toggle Track
                        sm.currentTrack =
                                if (sm.currentTrack ==
                                                com.typingtoucan.systems.SoundManager.MusicTrack
                                                        .WHAT
                                )
                                        com.typingtoucan.systems.SoundManager.MusicTrack.DARK_FOREST
                                else com.typingtoucan.systems.SoundManager.MusicTrack.WHAT
                    }
                    3 -> { // Back
                        pauseState = PauseState.MAIN
                        menuSelectedIndex = 3
                    }
                }
            }
        }
    }
}
