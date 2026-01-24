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

/**
 * The core gameplay screen.
 *
 * Orchestrates the game loop, including entity updates ([Bird], [Neck]),
 * typing queue processing, rendering, and collision detection.
 *
 * @param game The main game instance.
 * @param difficulty The chosen [DifficultyManager.Difficulty] level.
 * @param isPracticeMode Whether the game is running in practice mode (no death).
 * @param customSource Optional custom text source for typing content.
 * @param isAutoplay Whether the game is running in autoplay mode (AI types for you).
 */
class GameScreen(
        private val game: TypingToucanGame,
        var difficulty: DifficultyManager.Difficulty = DifficultyManager.Difficulty.NORMAL,
        private val isPracticeMode: Boolean = false,
        private val customSource: com.typingtoucan.systems.TypingSource? = null,
        private val isAutoplay: Boolean = false,
        private val startLevel: Int = 1,
        private val isArcadeMode: Boolean = false
) : Screen, InputProcessor {
    private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 600f) }
    private val viewport = com.badlogic.gdx.utils.viewport.ExtendViewport(800f, 600f, camera)

    // Font assets
    private var uiFont: com.badlogic.gdx.graphics.g2d.BitmapFont
    private var queueFont: com.badlogic.gdx.graphics.g2d.BitmapFont
    private val layout = GlyphLayout()
    private var smallFont: com.badlogic.gdx.graphics.g2d.BitmapFont

    // Graphics assets
    private val backgroundTiles = mutableListOf<Texture>()
    private lateinit var groundTexture: TextureRegion
    private lateinit var victoryTexture: TextureRegion
    private lateinit var monkeyTextures: List<TextureRegion>
    private lateinit var currentMonkeyTexture: TextureRegion

    // Ground Animation
    private lateinit var groundAnimTextures: List<TextureRegion>
    private lateinit var groundAnimation: Animation<TextureRegion>
    private var activeGroundIndex = 0
    private var groundTimer = 0f

    private enum class GroundState {
        IDLE,
        ANIMATING
    }

    // Single Sprite Obstacles (50x300)
    private lateinit var giraffeObstacles: Array<TextureRegion>
    private lateinit var anacondaObstacles: Array<TextureRegion>

    // Toucan Assets
    private lateinit var toucanPainTexture: TextureRegion

    // Animation
    private val birdAnimation: Animation<TextureRegion>
    private var stateTime = 0f

    // Game State
    private val shapeRenderer = ShapeRenderer()
    private val bird = Bird(90f, 400f)
    private val neckPool = object : com.badlogic.gdx.utils.Pool<Neck>() {
        override fun newObject(): Neck = Neck()
    }
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
        VICTORY,
        EXIT_CONFIRM
    }
    private var pauseState: PauseState = PauseState.NONE

    private val mainMenuItems =
            if (customSource is com.typingtoucan.systems.CustomPoolSource && !isArcadeMode) {
                listOf("Resume", "Difficulty", "Audio", "Letter Selection Menu", "Main Menu")
            } else if (customSource is com.typingtoucan.systems.TextSnippetSource || isArcadeMode) {
                listOf("Resume", "Difficulty", "Audio", "Main Menu")
            } else {
                listOf("Resume", "Difficulty", "Capitals", "Audio", "Main Menu")
            }
    private val audioMenuItems = listOf("Sound", "Music", "Music Track", "Back")
    private val exitConfirmItems = listOf("Yes", "No")
    private var menuSelectedIndex = 0
    private val soundManager = game.soundManager

    private var neckTimer = 0f
    private var nextNeckInterval = diffManager.neckInterval // Initialize with default

    private val SELECTED_COLOR = Color(1f, 0.906f, 0f, 1f) // #ffe700
    private var score = 0
    private var progressionPoints = 0
    private var displayProgression = 0f // Fluid animation value
    // private var gameOver = false // Removed for robustness
    private var gameStarted = false
    private var hurtTimer = 0f // Timer for red flash effect
    private var flashTimer = 0f // Timer for green "level up" flash
    private var justUnlockedChar = ""
    private var milestoneTimer = 0f // Timer for milestone "pulse" effect
    private var textAnimTimer = 1f
    private var lastLineIndex = -1
    private var level = startLevel
    private var totalNecksSpawned = 0
    private var autoplayTimer = 0f
    private var aiFlapCooldown = 0f

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
    
    // High Score Display
    private var topHighScore = 0

    // Monkey Decoration
    private var monkeyX = 500f
    private var monkeyPassed = false


    // Cached Draw Strings
    private var cachedQueueStr = ""
    private var cachedLevelStr = startLevel.toString()
    private var cachedWeightStr = ""
    private val descenders = listOf("g", "j", "p", "q", "y") // Static allocation

    // Performance Caches
    private val tempVec = com.badlogic.gdx.math.Vector3() // Reusable Vector3
    private val tempColor = Color() // Reusable color object
    private var cachedMarkupLine = "" // For Text Mode
    private var lastLocalProgress = -1 // For Text Mode

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
        parameter.size = 26 // Increased for mobile
        parameter.color = Color.WHITE
        parameter.borderColor = Color.BLACK
        parameter.borderWidth = 2f
        uiFont = generator.generateFont(parameter)

        // Create large font for queue display
        val queueParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        queueParam.size = 70 // Increased for mobile
        queueParam.color = Color.WHITE // Will be updated dynamically
        queueParam.borderColor = Color.BLACK
        queueParam.borderWidth = 4f // Thicker border
        // Shadow removed
        queueFont = generator.generateFont(queueParam)

        // Create smaller font for "LEVEL" label
        val labelParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        labelParam.size = 18 // Increased for mobile
        labelParam.color = Color.WHITE
        labelParam.borderColor = Color.BLACK
        labelParam.borderWidth = 1f
        smallFont = generator.generateFont(labelParam)

        generator.dispose()

         // Retrieve Assets from Atlas
         val atlas = game.assetManager.get("assets/atlas/game.atlas", com.badlogic.gdx.graphics.g2d.TextureAtlas::class.java)

         for (i in 0..9) {
             backgroundTiles.add(game.assetManager.get("assets/background_tiles/tile_$i.png", Texture::class.java))
         }
         groundTexture = atlas.findRegion("ground")
         
         // Big Textures (Direct loads converted to Regions)
         val victoryTex = game.assetManager.get("assets/victory_background.png", com.badlogic.gdx.graphics.Texture::class.java)
         victoryTexture = TextureRegion(victoryTex)

         val groundFrames = com.badlogic.gdx.utils.Array<TextureRegion>()
         val anim1 = atlas.findRegion("ground_anim", 1)
         val anim2 = atlas.findRegion("ground_anim", 2)
         groundAnimTextures = listOf(anim1, anim2)
         groundFrames.add(anim1)
         groundFrames.add(anim2)
         groundAnimation = Animation(0.8f, groundFrames, Animation.PlayMode.LOOP)

         monkeyTextures = listOf(
             atlas.findRegion("banana_monkey"),
             atlas.findRegion("banana_monkey", 1),
             atlas.findRegion("banana_monkey", 2),
             atlas.findRegion("banana_monkey", 3),
             atlas.findRegion("young_monkey"),
             atlas.findRegion("young_monkey", 1),
             atlas.findRegion("young_monkey", 2),
             atlas.findRegion("young_monkey", 3),
             atlas.findRegion("old_monkey"),
             atlas.findRegion("old_monkey", 1),
             atlas.findRegion("old_monkey", 2),
             atlas.findRegion("old_monkey", 3)
         )
         currentMonkeyTexture = monkeyTextures.random()

         giraffeObstacles = Array(5) { i -> atlas.findRegion("giraffe${i + 1}") }
         anacondaObstacles = Array(2) { i -> atlas.findRegion("anaconda_long", i) }
         toucanPainTexture = atlas.findRegion("toucan_pain")

         // Setup Bird Animation
         val birdRegions = com.badlogic.gdx.utils.Array<TextureRegion>(4)
         for (i in 0 until 4) {
             birdRegions.add(atlas.findRegion("toucan", i))
         }
         birdAnimation = Animation(0.1f, birdRegions, Animation.PlayMode.LOOP)

        updateQueueString()

        // Apply initial difficulty gravity and flap strength
        bird.gravity = diffManager.gravity
        bird.flapStrength = diffManager.flapStrength

        // Initial Monkey Pos for first run
        val scale = 150f / currentMonkeyTexture.regionHeight.toFloat()
        val mWidth = currentMonkeyTexture.regionWidth * scale
        val distanceToSpawn = diffManager.scrollSpeed * nextNeckInterval
        monkeyX = viewport.worldWidth + distanceToSpawn - 90f - mWidth
        monkeyPassed = false

        // Initialize Performance Caches
        practiceModeLayout.setText(uiFont, "Practice Mode - Obstacles Disabled")
        startTextLayout.setText(queueFont, "TYPE TO START")
        escTextLayout.setText(uiFont, "Press ESC for Menu")
        victoryTextLayout.setText(queueFont, "VICTORY!")
        victorySubTextLayout.setText(uiFont, "Press ENTER to Return to Menu")

        // Sync Capitals Preference
        typingQueue.setCapitalsEnabled(com.typingtoucan.utils.SaveManager.loadCapitalsEnabled())

        // Fast Forward Level (Learning Mode)
        if (startLevel > 1) {
            repeat(startLevel - 1) {
                typingQueue.expandPool()
            }
        }
        
        // Initialize High Scores
        if (isArcadeMode) {
            topHighScore = com.typingtoucan.utils.SaveManager.getArcadeStreak()
            maxStreak = topHighScore
        } else if (isPracticeMode) {
            if (customSource is com.typingtoucan.systems.CustomPoolSource) {
                 topHighScore = com.typingtoucan.utils.SaveManager.getCustomStreak()
            } else {
                 topHighScore = com.typingtoucan.utils.SaveManager.getTextStreak()
            }
            maxStreak = topHighScore // Init session max to global max
        } else {
            topHighScore = com.typingtoucan.utils.SaveManager.getNormalLevel()
        }
    }

    private fun updateQueueString() {
        cachedQueueStr = typingQueue.queue.joinToString("   ")
    }

    override fun show() {
        Gdx.input.inputProcessor = this
        updateQueueString()

        // Switch to Game Music but don't play yet (wait for gameStarted)
        // Apply pending track selection
        if (game.soundManager.currentTrack != game.soundManager.pendingTrack) {
             game.soundManager.currentTrack = game.soundManager.pendingTrack
        }
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
                 // Calculate effective level (penalize skipped levels)
                 val effectiveLevel = level - (startLevel - 1)
                 if (effectiveLevel > 0) {
                     com.typingtoucan.utils.SaveManager.saveNormalLevel(effectiveLevel)
                 }
            }
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Error during endGame", e)
        } finally {
            softReset()
        }
    }


    override fun render(delta: Float) {
        // Handle Input for Pause/Back
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            if (pauseState == PauseState.VICTORY) {
                game.screen = MenuScreen(game)
            } else if (pauseState == PauseState.NONE) {
                pauseState = PauseState.MAIN
                menuSelectedIndex = 0
                game.soundManager.pauseMusic()
            } else if (pauseState == PauseState.AUDIO) {
                pauseState = PauseState.MAIN
                menuSelectedIndex = 3
            } else if (pauseState == PauseState.MAIN) {
                pauseState = PauseState.EXIT_CONFIRM
                menuSelectedIndex = 1 // Default to 'No'
            } else if (pauseState == PauseState.EXIT_CONFIRM) {
                game.soundManager.stopMusic()
                game.screen = MenuScreen(game)
            } else {
                pauseState = PauseState.NONE
                game.soundManager.resumeMusic()
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
            // Auto-Start for Credits Mode
            if (!gameStarted && isAutoplay && pauseState == PauseState.NONE) {
                // Determine start character (first in queue)
                if (typingQueue.queue.isNotEmpty()) {
                    val startChar = typingQueue.queue.first()
                    // Simulate key press
                    keyTyped(startChar)
                }
            }

            // If paused or ready state, animate bird in place?
            if (pauseState == PauseState.NONE) {
                // Ready state (Type to Start)
                stateTime += delta
                bird.y = 300f + kotlin.math.sin(stateTime * 5f) * 10f
                bird.velocity = 0f
            } else {
                handlePauseMenuInput()
                if (Gdx.input.justTouched()) {
                    handlePauseMenuTouch()
                }
            }
        }

        draw(delta)
    }

    private fun update(delta: Float) {
        if (isAutoplay) {
             autoplayTimer -= delta
             if (autoplayTimer <= 0) {
                  autoplayTimer = 0.11f // Approx 9 chars/sec (Further slowed)
                  if (typingQueue.queue.isNotEmpty()) {
                      keyTyped(typingQueue.queue.first())
                  }
             }
             
             if (gameStarted) {
                 // Simple Flap AI
                 // Find target pipe gap
                 var targetY = viewport.worldHeight / 2f
                 // Look for closest pipe ahead
                 for (neck in necks) {
                     // small buffer on X (bird.x + bird.width)
                     if (neck.x + neck.width > bird.x) {
                         // Target the LOWER part of the gap to allow for the Flap Arc (approx 100px rise).
                         // Gap Center Y - 180f puts trigger point low, peaking near center.
                         targetY = neck.gapCenterY - 140f 
                         break
                     }
                 }
                 
                 // Flap Logic: Physics-based
                 if (bird.y < targetY) {
                     // Only flap if we are:
                     // 1. Falling (velocity < 0) - maintain height
                     // 2. Significantly below target and needing momentum (velocity < 3f)
                     if (aiFlapCooldown <= 0 && (bird.velocity < 0 || (bird.y < targetY - 60 && bird.velocity < 3f))) {
                         bird.flap()
                         // aiFlapCooldown = 0.2f // Optional small cooldown
                     }
                 }
                 if (aiFlapCooldown > 0) aiFlapCooldown -= delta
             }
        }

        bird.update()
        stateTime += delta

        // Update hurt timer
        if (hurtTimer > 0) {
            hurtTimer -= delta
            if (hurtTimer < 0) hurtTimer = 0f
        }

        // Ground Animation Logic (Random Frame every 1-3s)
        groundTimer -= delta
        if (groundTimer <= 0) {
             // Pick random duration 1-3s
             groundTimer = 1f + Math.random().toFloat() * 2f
             
             // Pick different frame
             // 0 = Base, 1 = Anim1, 2 = Anim2
             val oldIndex = activeGroundIndex
             var newIndex = oldIndex
             while (newIndex == oldIndex) {
                 newIndex = (0..2).random()
             }
             activeGroundIndex = newIndex
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
        
        // Monkey Sound Logic
        if (!monkeyPassed && monkeyX < bird.x) {
            monkeyPassed = true
            soundManager.playMonkey()
        }

        val tileScale = viewport.worldHeight / 1200f
        val totalBgWidth = 1024f * backgroundTiles.size * tileScale

        if (backgroundX <= -totalBgWidth) {
            backgroundX += totalBgWidth
        }
        if (groundX <= -groundTexture.regionWidth.toFloat()) groundX = 0f
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
            val neck = neckPool.obtain()
            neck.init(viewport.worldWidth + 50f, gapY, isSnake)
            necks.add(neck)
            totalNecksSpawned++
        }

        for (i in necks.size - 1 downTo 0) {
            val neck = necks[i]
            neck.update(delta, diffManager.scrollSpeed)

            if (neck.x + neck.width < 0) {
                neckPool.free(neck)
                necks.removeIndex(i)
                continue
            }

            if (!neck.scored && neck.x < bird.x) {
                neck.scored = true
                score++
                soundManager.playScore()
                progressionPoints++
                if (progressionPoints >= 5) {
                    var didLevelUp = false
                    
                    if (typingQueue.isFullyUnlocked()) {
                        // Max Level reached: Continue leveling endlessly
                        didLevelUp = true
                        justUnlockedChar = "MAX"
                    } else {
                        val unlockedChars = typingQueue.expandPool()
                        if (unlockedChars.isNotEmpty()) {
                             didLevelUp = true
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
                             
                             // Check for milestone (only during progression)
                             if (level % 5 == 0) {
                                 milestoneTimer = 2.0f // Pulse for 2 seconds (starts loop)
                             }
                        }
                    }

                    if (didLevelUp) {
                         level++
                         if (!isPracticeMode) {
                             val effectiveLevel = level - (startLevel - 1)
                             if (effectiveLevel > topHighScore) {
                                  topHighScore = effectiveLevel
                                  com.typingtoucan.utils.SaveManager.saveNormalLevel(effectiveLevel)
                             }
                         }
                         cachedLevelStr = level.toString() // Cache Update
                         flashTimer = 1.0f // Flash green for 1.0 second
                         soundManager.playLevelUp()
                    }
                    progressionPoints = 0
                }
            }

            if (!neck.collided &&
                            (Intersector.overlaps(bird.bounds, neck.bottomBounds) ||
                                     Intersector.overlaps(bird.bounds, neck.topBounds))
            ) {
                if (!isPracticeMode || customSource is com.typingtoucan.systems.TextSnippetSource) {
                    // Neck collision is now non-fatal for Practice/Text too (just penalizes)
                    neck.collided = true
                    soundManager.playCrash()
                    hurtTimer = 0.5f // Flash red and show pain sprite

                    // RECOIL: Bounce bird away from obstacle
                    if (com.badlogic.gdx.math.Intersector.overlaps(bird.bounds, neck.topBounds)) {
                        bird.velocity = -5f // Push Down (hit ceiling/top pipe)
                    } else {
                        bird.velocity = 7f // Push Up (hit floor/bottom pipe)
                    }

                    if (isPracticeMode) {
                        // Text Mode Logic
                        streak = 0
                    } else {
                        // Normal Mode Penalty
                        if (typingQueue.queue.isNotEmpty()) {
                            progressionPoints = (progressionPoints - 1).coerceAtLeast(0)
                        }
                    }
                }
            }
        }

        if (bird.bounds.y <= 60) { // Screen height for ground is 60
            if (isPracticeMode) {
                if (customSource is com.typingtoucan.systems.TextSnippetSource) {
                     // Text Mode: Soft Reset (Type to Start) behavior
                     soundManager.playCrash()
                     softReset()
                } else {
                     // Practice Mode: Bounce
                     streak = 0
                     bird.y = 80f // Reset slightly above ground
                     bird.velocity = 0f
                     bird.flap()
                     soundManager.playFlap()
                }
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
        
        neckPool.freeAll(necks)
        necks.clear()
        hurtTimer = 0f
        flashTimer = 0f

        // Reset Neck Logic for determinstic spawn
        neckTimer = 0f
        nextNeckInterval = diffManager.neckInterval

        // Position Monkey: Screen End + Distance to spawn - 90 buffer - MonkeyWidth
        currentMonkeyTexture = monkeyTextures.random()
        val scale = 150f / currentMonkeyTexture.regionHeight.toFloat()
        val mWidth = currentMonkeyTexture.regionWidth * scale
        val distanceToSpawn = diffManager.scrollSpeed * nextNeckInterval
        monkeyX = viewport.worldWidth + distanceToSpawn - 90f - mWidth
        monkeyPassed = false

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
        // Draw Background (Tiled with Culling)
        val worldH = viewport.worldHeight
        // Proportional Scaling based on height
        val tileScale = worldH / 1200f // Base height of tiles is 1200
        val baseTileW = 1024f * tileScale
        val totalBgWidth = baseTileW * backgroundTiles.size

        // We draw two instances of the total panorama to loop
        for (loop in 0..1) {
            val loopOffset = loop * totalBgWidth
            backgroundTiles.forEachIndexed { index, tile ->
                val tileX = backgroundX + loopOffset + (index * baseTileW)
                
                // Culling: Only draw if visible
                if (tileX + baseTileW > 0 && tileX < viewport.worldWidth) {
                    game.batch.draw(tile, tileX, 0f, baseTileW, worldH)
                }
            }
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
        val groundW = groundTexture.regionWidth.toFloat()
        val numGroundTiles = kotlin.math.ceil(viewport.worldWidth / groundW).toInt() + 1

        val textureToDraw =
                when (activeGroundIndex) {
                    0 -> groundTexture
                    1 -> groundAnimTextures[0]
                    else -> groundAnimTextures[1]
                }

        for (i in 0 until numGroundTiles) {
            game.batch.draw(textureToDraw, groundX + i * groundW, 0f, groundW, 70f)
        }

        // Draw Monkey (if visible)
        if (monkeyX > -200) { // Simple culling
            val scale = 150f / currentMonkeyTexture.regionHeight.toFloat()
            val width = currentMonkeyTexture.regionWidth * scale
            game.batch.draw(currentMonkeyTexture, monkeyX, 40f, width, 150f)
        }

        // Draw Bird
        val currentFrame =
                if (hurtTimer > 0) {
                    toucanPainTexture // Use regional texture directly
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
            tempColor.set(1f, 0f, 0f, 0.5f * (hurtTimer / 0.3f))
            shapeRenderer.color = tempColor
            shapeRenderer.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        }

        if (!isPracticeMode) {
            // 2. Status Bar
            var barWidth = 200f
            var barHeight = 24f // Slightly thicker
            var barX = viewport.worldWidth / 2f - barWidth / 2f
            var barY = viewport.worldHeight - 70f // Increased padding from top
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
        // 4. Underscore (If applicable) OR Text Mode Display
        // 4. Underscore (If applicable)
        // 4. Underscore (If applicable)
        // 4. Underscore (If applicable)
        val src = customSource
        // Only draw underscore if NOT in TextSnippet mode (Standard Mode)
        if (pauseState == PauseState.NONE && typingQueue.queue.isNotEmpty() && src !is com.typingtoucan.systems.TextSnippetSource) {
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
                    if (descenders.any { firstCharStr.contains(it, ignoreCase = true) }) 85f
                    else 70f
            shapeRenderer.rect(rectX, textY - yOffset, underscoreWidth, 5f)
        }
        // Text Mode Underscore (Neon Pink)
        else if (pauseState == PauseState.NONE && src is com.typingtoucan.systems.TextSnippetSource) {
             val state = src.getDisplayState()
             
             // Replicate Animation Logic
             val animT = if (state.lineIndex != lastLineIndex && lastLineIndex != -1) 0f else textAnimTimer
             // Using approximate consistent timer or just textAnimTimer frame lag
             val progressFactor = (textAnimTimer / 0.5f).coerceIn(0f, 1f)
             val t = progressFactor * (2 - progressFactor)
             val shiftY = -70f * (1f - t)
             val startY = viewport.worldHeight / 2f + 50f + shiftY
             
             // Metrics (Raw font)
             layout.setText(queueFont, state.currentLine)
             val fullWidth = layout.width
             val startX = viewport.worldWidth / 2f - fullWidth / 2f
             
             val typed = state.currentLine.take(state.localProgress)
             layout.setText(queueFont, typed)
             val typedWidth = layout.width
             
             // Cursor
             val cursorX = startX + typedWidth
             var charWidth = 20f
             if (state.localProgress < state.currentLine.length) {
                 val nextChar = state.currentLine[state.localProgress].toString()
                 layout.setText(queueFont, nextChar)
                 charWidth = layout.width
             }
             if (charWidth < 10f) charWidth = 15f // Min width for space/thin chars check
             
             // Neon Pink Glow (Hot Pink)
             // Layered Rects for Glow?
             val alpha = 0.6f + 0.4f * kotlin.math.sin(stateTime * 10f)
             shapeRenderer.color = Color(1f, 0.41f, 0.71f, alpha) // HotPink
             
             val underlineY = startY - 60f // Below baseline (corrected from -10f which was too high)
             
             // Core
             shapeRenderer.rect(cursorX, underlineY, charWidth, 4f)
             // Glow (Wider, Fainter)
             shapeRenderer.color = Color(1f, 0.41f, 0.71f, alpha * 0.5f)
             shapeRenderer.rect(cursorX - 2f, underlineY - 2f, charWidth + 4f, 8f)
        }

        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        // --- BATCH PHASE 2: UI & TEXT ---
        game.batch.begin()

        // --- SPECIAL TEXT MODE RENDERING ---
        if (pauseState == PauseState.NONE && customSource is com.typingtoucan.systems.TextSnippetSource) {
            val src = customSource
            val state = src.getDisplayState()
            val metadata = src.sourceMetadata

            // Animation Logic
            // We use a persistent var 'textAnimOffset' stored in GameScreen (assume accessible or use local hack)
            // Wait, I cannot add a property to GameScreen via replace_file_content easily without context.
            // I will use `stateTime` relative to a simpler check? No.
            // I need a property. If I can't add one, checking lineIndex change is hard.
            
            // Wait, I can't easily add `private var lastTextLineIndex = -1` and `private var textAnimY = 0f` to the CLASS.
            // But I can use the `isPracticeMode` block later? No.
            // I have to add properties to the CLASS.
            // I will do that in a separate chunk or tool call if needed.
            // But I am in `replace_file_content`.
            // I will assume I can't persist state easily without editing class body.
            // Let's settle for INSTANT transition for now if I can't add state?
            // "When a line is finished... scrolls up".
            // I MUST add state variables.
            
            // Proceed assuming I will add them in `GameScreen` class body separately.
            // Variables: textAnimTimer, textAnimDuration, lastLineIndex
            
            // Update Animation
            if (state.lineIndex != lastLineIndex) {
                 if (lastLineIndex != -1) {
                     textAnimTimer = 0f // Start animation
                 }
                 lastLineIndex = state.lineIndex
            }
            
            textAnimTimer += Gdx.graphics.deltaTime
            val progressFactor = (textAnimTimer / 0.5f).coerceIn(0f, 1f)
            // Interpolation: Pow2Out or similar
            // We want to scroll UP.
            // Visual mapping: 
            // Normalized Position 0 = Target (Standard).
            // Normalized Position 1 = Start (Previous Line Position).
            // Actually simpler:
            // currentY = lerp(-70f, 0f, progressFactor)
            // prevY = lerp(0f, 70f, progressFactor)
            // nextY = lerp(-140f, -70f, progressFactor)
            
            // Use smooth step
            val t = progressFactor * (2 - progressFactor) // QuadOut
            val shiftY = -70f * (1f - t) // Starts at -70, goes to 0
            
            // Metadata display removed per user request
            // if (metadata.isNotEmpty()) ...

            // Base Position
            val startY = viewport.worldHeight / 2f + 50f
            
            // Enable markup for precise alignment
            queueFont.data.markupEnabled = true

            // Helper to draw centered line optimized for GC
            fun drawLine(text: String, y: Float, alpha: Float = 1f, isCurrent: Boolean = false, localProg: Int = 0) {
                 if (text.isEmpty()) return
                 
                 // Measure full width (raw text)
                 layout.setText(queueFont, text)
                 val fullWidth = layout.width
                 val startX = viewport.worldWidth / 2f - fullWidth / 2f

                 if (isCurrent && localProg > 0) {
                     // Draw Green (Typed) Part
                     queueFont.color = tempColor.set(0.2f, 1f, 0.2f, alpha) // #33FF33
                     queueFont.draw(game.batch, text, startX, y, 0, localProg, 0f, com.badlogic.gdx.utils.Align.left, false)
                     
                     // Measure Typed Width to offset remaining part
                     layout.setText(queueFont, text, 0, localProg, queueFont.color, 0f, com.badlogic.gdx.utils.Align.left, false, null)
                     val typedWidth = layout.width

                     // Draw White (Remaining) Part
                     queueFont.color = tempColor.set(1f, 1f, 1f, alpha)
                     if (localProg < text.length) {
                         queueFont.draw(game.batch, text, startX + typedWidth, y, localProg, text.length, 0f, com.badlogic.gdx.utils.Align.left, false)
                     }
                 } else {
                     // Draw Entire Line Dim (if not current) or pure white (if current but progress 0)
                     val baseAlpha = if (isCurrent) 1f else 0.4f
                     queueFont.color = tempColor.set(1f, 1f, 1f, baseAlpha * alpha)
                     queueFont.draw(game.batch, text, startX, y, 0, text.length, 0f, com.badlogic.gdx.utils.Align.left, false)
                 }
                 queueFont.color = Color.WHITE
            }

            // Draw Previous (Fading out and moving up)
            // Pos: 0 -> +70 (Relative to StartY)
            // But we apply shiftY to current.
            // If shiftY goes -70 -> 0.
            // PrevY should equal CurrentY + 70.
            // So PrevY starts at 0, goes to +70. Correct.
            // Draw Previous (Fading out and moving up)
            if (state.prevLine.isNotEmpty() && progressFactor < 1f) {
                drawLine(state.prevLine, startY + shiftY + 70f, alpha = 1f - progressFactor)
            }
            
            // Draw Current
            drawLine(state.currentLine, startY + shiftY, isCurrent = true, localProg = state.localProgress)
            
            // Draw Next
            if (state.nextLine.isNotEmpty()) {
                drawLine(state.nextLine, startY + shiftY - 70f, alpha = 0.5f) // Always dim
            }
        }

        if (isPracticeMode && customSource !is com.typingtoucan.systems.TextSnippetSource) {
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
        // HUD is hidden in Autoplay (Credits)
        if (!isAutoplay) {
             // Shifted from right edge to properly center labels on mobile
             val levelCommonCenter = viewport.worldWidth - 70f
             
             if (isPracticeMode || isArcadeMode) {
                 // --- PRACTICE/TEXT/ARCADE MODE ---
                 // Top: BEST STREAK
                 val maxLabel = "HIGH SCORE"
                 layout.setText(smallFont, maxLabel)
                 smallFont.draw(game.batch, maxLabel, levelCommonCenter - layout.width / 2, viewport.worldHeight - 30f)
                 
                 val maxVal = topHighScore.toString()
                 // Use queueFont scaled down
                 queueFont.data.setScale(0.5f)
                 layout.setText(queueFont, maxVal)
                 queueFont.draw(game.batch, maxVal, levelCommonCenter - layout.width / 2, viewport.worldHeight - 60f)
                 queueFont.data.setScale(1.0f)

                 // Bottom: CURRENT STREAK
                 val streakLabel = "STREAK"
                 layout.setText(smallFont, streakLabel)
                 smallFont.draw(game.batch, streakLabel, levelCommonCenter - layout.width / 2, 90f)

                 // Pulse Effect for Streak Value
                 if (milestoneTimer > 0) {
                     val pulse = kotlin.math.abs(kotlin.math.sin(stateTime * 5f))
                     val scale = 1.0f + 0.4f * pulse
                     queueFont.data.setScale(scale)
                     val colorPulse = (kotlin.math.sin(stateTime * 5f) + 1f) / 2f
                     tempColor.set(Color.GOLD).lerp(Color.CYAN, colorPulse)
                     queueFont.color = tempColor
                 }

                 val streakVal = streak.toString()
                 layout.setText(queueFont, streakVal)
                 queueFont.draw(game.batch, streakVal, levelCommonCenter - layout.width / 2, 60f)
                 
                 queueFont.data.setScale(1.0f)
                 queueFont.color = Color.WHITE

             } else {
                 // --- NORMAL MODE ---
                 // Top: BEST LEVEL
                 val bestLabel = "HIGH SCORE"
                 layout.setText(smallFont, bestLabel)
                 smallFont.draw(game.batch, bestLabel, levelCommonCenter - layout.width / 2, viewport.worldHeight - 30f)
                 
                 val bestVal = topHighScore.toString()
                 queueFont.data.setScale(0.5f)
                 layout.setText(queueFont, bestVal)
                 queueFont.draw(game.batch, bestVal, levelCommonCenter - layout.width / 2, viewport.worldHeight - 60f)
                 queueFont.data.setScale(1.0f)

                 // Bottom: CURRENT LEVEL
                 val levelLabel = "LEVEL"
                 layout.setText(smallFont, levelLabel)
                 smallFont.draw(game.batch, levelLabel, levelCommonCenter - layout.width / 2, 100f)

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
            val menuTitle = if (pauseState == PauseState.EXIT_CONFIRM) "Exit to main menu. Are you sure?" else "PAUSED"
            layout.setText(uiFont, menuTitle)
            uiFont.draw(
                    game.batch,
                    menuTitle,
                    viewport.worldWidth / 2f - layout.width / 2,
                    viewport.worldHeight - 100f
            )

            if (pauseState == PauseState.MAIN) drawPauseMainMenu()
            else if (pauseState == PauseState.AUDIO) drawAudioMenu()
            else if (pauseState == PauseState.EXIT_CONFIRM) drawExitConfirmMenu()
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
        // Queue Text
        if (pauseState == PauseState.NONE && customSource !is com.typingtoucan.systems.TextSnippetSource) {
            // Standard single-line queue
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

            // Fix for Text Mode: Ensure cursor advances even if updateWeights is false
            if (isPracticeMode && customSource is com.typingtoucan.systems.TextSnippetSource) {
                 customSource.onCharTyped(character)
            }

            // Update Cache
            updateQueueString()
            weightFlashValue = result
            cachedWeightStr = weightFlashValue.toString()

            weightFlashTimer = 1.0f
            weightFlashTimer = 1.0f
            
            // Only flap on key press if NOT in Autoplay mode.
            // In Autoplay, the Flight AI handles flapping.
            if (!isAutoplay) {
                bird.flap()
                soundManager.playFlap()
            }

            if (isPracticeMode || isArcadeMode) {
                streak++
                if (!isAutoplay && streak > topHighScore) {
                    topHighScore = streak
                    if (isArcadeMode) {
                         com.typingtoucan.utils.SaveManager.saveArcadeStreak(streak)
                    } else if (customSource is com.typingtoucan.systems.CustomPoolSource) {
                        com.typingtoucan.utils.SaveManager.saveCustomStreak(streak)
                    } else {
                        com.typingtoucan.utils.SaveManager.saveTextStreak(streak)
                    }
                }
                if (streak > maxStreak) maxStreak = streak

                // Flash every 10 streaks
                if (streak % 10 == 0 && !isAutoplay) {
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

                if (isPracticeMode || isArcadeMode) {
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
    override fun pause() {
        if (gameStarted && pauseState == PauseState.NONE) {
            pauseState = PauseState.MAIN
            menuSelectedIndex = 0
        }
        game.soundManager.pauseMusic()
    }

    override fun resume() {
        game.soundManager.resumeMusic()
    }
    override fun hide() {}
    override fun dispose() {
        uiFont.dispose()
        smallFont.dispose()
        queueFont.dispose()
        // generator.dispose() // Generator is disposed in init now
        shapeRenderer.dispose()
        neckPool.clear()

        // Textures needed to be disposed manually before AssetManager.
        // Now AssetManager owns them. We do NOT dispose them here.
        // TappyBirdGame disposes AssetManager on exit.

        // soundManager is owned by Game, do not dispose here
        // Textures are owned by AssetManager/Atlas, do not dispose here
    }

    private fun drawPauseMainMenu() {
        var startY = viewport.worldHeight / 2f + 100f
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
            uiFont.color = if (isSelected) SELECTED_COLOR else Color.WHITE

            layout.setText(uiFont, label)
            val x = centerX - layout.width / 2
            val y = startY - (index * gap)
            uiFont.draw(game.batch, label, x, y)
        }
        uiFont.color = Color.WHITE
    }

    private fun drawAudioMenu() {
        var startY = viewport.worldHeight / 2f + 100f
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
            uiFont.color = if (isSelected) SELECTED_COLOR else Color.WHITE

            layout.setText(uiFont, label)
            uiFont.draw(game.batch, label, centerX - layout.width / 2, startY - (index * gap))
        }
        uiFont.color = Color.WHITE
    }

    private fun handlePauseMenuInput() {
        if (pauseState == PauseState.NONE) return

        val currentListSize =
                when (pauseState) {
                    PauseState.MAIN -> mainMenuItems.size
                    PauseState.AUDIO -> audioMenuItems.size
                    PauseState.EXIT_CONFIRM -> exitConfirmItems.size
                    else -> 0
                }

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
                    game.soundManager.resumeMusic()
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
                } else if (selectedItem == "Audio") {
                    pauseState = PauseState.AUDIO
                    menuSelectedIndex = 0
                } else if (selectedItem == "Main Menu") {
                    pauseState = PauseState.EXIT_CONFIRM
                    menuSelectedIndex = 1 // Default to 'No'
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
            } else if (pauseState == PauseState.EXIT_CONFIRM) {
                if (menuSelectedIndex == 0) { // Yes
                    game.soundManager.stopMusic()
                    game.screen = MenuScreen(game)
                } else { // No
                    pauseState = PauseState.MAIN
                    menuSelectedIndex = mainMenuItems.indexOf("Main Menu").coerceAtLeast(0)
                }
            }
        }
    }
    
    private fun handlePauseMenuTouch() {
         val touchX = Gdx.input.x.toFloat()
         val touchY = Gdx.input.y.toFloat()
         tempVec.set(touchX, touchY, 0f)
         val worldPos = camera.unproject(tempVec)
         
         val centerX = viewport.worldWidth / 2f
         var startY = viewport.worldHeight / 2f + 100f
         val gap = 50f
         
         val currentList =
                 if (pauseState == PauseState.MAIN) mainMenuItems else audioMenuItems
                 
         currentList.forEachIndexed { index, item ->
             val label =
                     when (item) {
                         "Difficulty" -> "Difficulty: ${difficulty.name}"
                         "Capitals" ->
                                 "Capitals: ${if (com.typingtoucan.utils.SaveManager.loadCapitalsEnabled()) "ON" else "OFF"}"
                         "Sound" -> "Sound: ${if (game.soundManager.soundEnabled) "ON" else "OFF"}"
                         "Music" -> "Music: ${if (game.soundManager.musicEnabled) "ON" else "OFF"}"
                         "Music Track" -> { // Approximate length check or just make hitbox wide
                             "Music Track: Dark Forest" 
                         }
                         else -> item
                     }
             
             layout.setText(uiFont, label) // Note: Dynamic labels might vary slightly in width but this is close enough for touch
             val w = layout.width + 40f // Padding
             val h = layout.height + 40f
             val x = centerX - w / 2
             val y = startY - (index * gap) - 20f // Adjust for baseline
             
             if (worldPos.x >= x && worldPos.x <= x + w &&
                 worldPos.y >= y && worldPos.y <= y + h) {
                     menuSelectedIndex = index
                     // Simulate Enter Press logic by calling the input handler logic again or refactoring.
                     // Since input handler uses Gdx.input.isKeyJustPressed, we can't easily fake it.
                     // We should extract the "Execute" logic.
                     executePauseMenuAction()
             }
         }
    }
    
    private fun executePauseMenuAction() {
            // Play Selection Sound
            game.soundManager.playMenuSelect()

            if (pauseState == PauseState.MAIN) {
                val selectedItem = mainMenuItems[menuSelectedIndex]
                if (selectedItem == "Resume") {
                    pauseState = PauseState.NONE
                    game.soundManager.resumeMusic()
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
            } else if (pauseState == PauseState.EXIT_CONFIRM) {
                if (menuSelectedIndex == 0) { // Yes
                    game.soundManager.stopMusic()
                    game.screen = MenuScreen(game)
                } else { // No
                    pauseState = PauseState.MAIN
                    menuSelectedIndex = mainMenuItems.indexOf("Main Menu").coerceAtLeast(0)
                }
            }
    }

    private fun drawExitConfirmMenu() {
        var startY = viewport.worldHeight / 2f + 50f
        val gap = 50f
        val centerX = viewport.worldWidth / 2f

        exitConfirmItems.forEachIndexed { index, item ->
            val isSelected = index == menuSelectedIndex
            uiFont.color = if (isSelected) SELECTED_COLOR else Color.WHITE

            layout.setText(uiFont, item)
            uiFont.draw(game.batch, item, centerX - layout.width / 2, startY - (index * gap))
        }
        uiFont.color = Color.WHITE
    }
}
