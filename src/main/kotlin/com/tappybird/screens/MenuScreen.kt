package com.tappybird.screens

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.tappybird.TappyBirdGame
import com.tappybird.systems.DifficultyManager
import com.tappybird.utils.SaveManager

class MenuScreen(private val game: TappyBirdGame) : Screen {
    private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 600f) }
    private val viewport = com.badlogic.gdx.utils.viewport.ExtendViewport(800f, 600f, camera)

    // Assets
    private lateinit var titleFont: BitmapFont
    private lateinit var menuFont: BitmapFont
    private val shapeRenderer = ShapeRenderer()
    private val layout = GlyphLayout()

    // Menu State
    private val options = listOf("Start Game", "Custom Keys", "Options", "Credits")
    private val difficultyOptions = listOf("Easy", "Normal", "Hard", "Insane", "Back")
    private val optionsMenuItems = listOf("Sound", "Music", "Music Track", "Back")
    private var selectedIndex = 0
    private var isDifficultySelect = false
    private var isOptionsSelect = false

    init {
        // Initialize Fonts
        val generator =
                FreeTypeFontGenerator(Gdx.files.internal("assets/OriginalSurfer-Regular.ttf"))

        val titleParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        titleParam.size = 60
        titleParam.color = Color.GOLD
        titleParam.borderColor = Color.BLACK
        titleParam.borderWidth = 3f
        titleFont = generator.generateFont(titleParam)

        val menuParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        menuParam.size = 30
        menuParam.color = Color.WHITE
        menuParam.borderColor = Color.BLACK
        menuParam.borderWidth = 2f
        menuFont = generator.generateFont(menuParam)

        generator.dispose()
    }

    override fun show() {
        // Reset selection on show
        selectedIndex = 0
        isDifficultySelect = false
        isOptionsSelect = false

        // Start Menu Music
        if (game.soundManager.musicEnabled) {
            game.soundManager.currentTrack =
                    com.tappybird.systems.SoundManager.MusicTrack.DARK_FOREST
        }
    }

    override fun render(delta: Float) {
        // Input Handling
        handleInput(delta)

        // Draw
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        game.batch.projectionMatrix = camera.combined
        shapeRenderer.projectionMatrix = camera.combined

        game.assetManager.update()
        game.batch.begin()

        if (game.assetManager.isLoaded("assets/title_background.png")) {
            val bg =
                    game.assetManager.get(
                            "assets/title_background.png",
                            com.badlogic.gdx.graphics.Texture::class.java
                    )
            game.batch.draw(bg, 0f, 0f, viewport.worldWidth, viewport.worldHeight)
        }

        // Title
        val centerX = viewport.worldWidth / 2f
        val topY = viewport.worldHeight - 50f

        layout.setText(titleFont, "TYPING TOUCAN")
        titleFont.draw(game.batch, "TYPING TOUCAN", centerX - layout.width / 2, topY)

        // High Score
        val scoreText = "High Score: ${SaveManager.getHighScore()}"
        layout.setText(menuFont, scoreText)
        menuFont.draw(game.batch, scoreText, centerX - layout.width / 2, topY - 70f)

        if (isDifficultySelect) {
            drawDifficultySelect()
        } else if (isOptionsSelect) {
            drawOptionsMenu()
        } else {
            drawMainMenu()
        }

        game.batch.end()
    }

    private fun drawMainMenu() {
        val centerX = viewport.worldWidth / 2f
        val centerY = viewport.worldHeight / 2f + 50f // Center verticalish
        var startY = centerY
        val gap = 50f

        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            menuFont.color = if (isSelected) Color.CYAN else Color.WHITE

            layout.setText(menuFont, option)
            val x = centerX - layout.width / 2
            val y = startY - (index * gap)

            menuFont.draw(game.batch, option, x, y)

            if (isSelected) {
                // Draw Red Underline
                game.batch.end()
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.color = Color.RED
                shapeRenderer.rect(x, y - 30f, layout.width, 3f)
                shapeRenderer.end()
                game.batch.begin()
            }
        }
        // Reset color
        menuFont.color = Color.WHITE
    }

    private fun drawDifficultySelect() {
        val centerX = viewport.worldWidth / 2f
        val centerY = viewport.worldHeight / 2f + 50f
        var startY = centerY
        val gap = 50f

        val prompt = "SELECT DIFFICULTY"
        layout.setText(menuFont, prompt)
        menuFont.draw(game.batch, prompt, centerX - layout.width / 2, centerY + 100f)

        difficultyOptions.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            menuFont.color = if (isSelected) Color.CYAN else Color.WHITE

            layout.setText(menuFont, option)
            val x = centerX - layout.width / 2
            val y = startY - (index * gap)

            menuFont.draw(game.batch, option, x, y)

            if (isSelected) {
                // Draw Red Underline
                game.batch.end()
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.color = Color.RED
                shapeRenderer.rect(x, y - 30f, layout.width, 3f)
                shapeRenderer.end()
                game.batch.begin()
            }
        }
        menuFont.color = Color.WHITE
    }

    private fun drawOptionsMenu() {
        val centerX = viewport.worldWidth / 2f
        val centerY = viewport.worldHeight / 2f + 50f
        var startY = centerY
        val gap = 50f

        val prompt = "OPTIONS"
        layout.setText(menuFont, prompt)
        menuFont.draw(game.batch, prompt, centerX - layout.width / 2, centerY + 100f)

        optionsMenuItems.forEachIndexed { index, item ->
            val label =
                    when (item) {
                        "Sound" -> "Sound: ${if (game.soundManager.soundEnabled) "ON" else "OFF"}"
                        "Music" -> "Music: ${if (game.soundManager.musicEnabled) "ON" else "OFF"}"
                        "Music Track" -> {
                            val trackName =
                                    if (game.soundManager.currentTrack ==
                                                    com.tappybird.systems.SoundManager.MusicTrack
                                                            .WHAT
                                    )
                                            "What"
                                    else "Dark Forest"
                            "Music Track: $trackName"
                        }
                        else -> item
                    }

            val isSelected = index == selectedIndex
            menuFont.color = if (isSelected) Color.CYAN else Color.WHITE

            layout.setText(menuFont, label)
            val x = centerX - layout.width / 2
            val y = startY - (index * gap)

            menuFont.draw(game.batch, label, x, y)

            if (isSelected) {
                game.batch.end()
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.color = Color.RED
                shapeRenderer.rect(x, y - 30f, layout.width, 3f)
                shapeRenderer.end()
                game.batch.begin()
            }
        }
        menuFont.color = Color.WHITE
    }

    // Removed drawDifficultySelect

    private fun handleInput(delta: Float) {
        val currentList =
                when {
                    isDifficultySelect -> difficultyOptions
                    isOptionsSelect -> optionsMenuItems
                    else -> options
                }

        // Navigation
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex--
            if (selectedIndex < 0) selectedIndex = currentList.size - 1
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex++
            if (selectedIndex >= currentList.size) selectedIndex = 0
        }

        // Selection
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                        Gdx.input.isKeyJustPressed(Input.Keys.A)
        ) {
            selectOption(selectedIndex)
        }

        // Back / Escape
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isDifficultySelect) {
                isDifficultySelect = false
                selectedIndex = 0
            } else if (isOptionsSelect) {
                isOptionsSelect = false
                selectedIndex = 0
            }
        }
    }

    private fun selectOption(index: Int) {
        if (!isDifficultySelect && !isOptionsSelect) {
            // Main Menu Selection
            when (index) {
                0 -> {
                    // Start Game -> Go to Difficulty Select
                    isDifficultySelect = true
                    selectedIndex = 1 // Default to Medium (0=Easy, 1=Medium) if desired, or 0.
                }
                1 -> {
                    // Custom Keys (Placeholder)
                }
                2 -> {
                    // Options
                    isOptionsSelect = true
                    selectedIndex = 0
                }
                else -> {
                    // Credits
                }
            }
        } else if (isDifficultySelect) {
            // Difficulty Selection
            when (index) {
                0 -> startGame(DifficultyManager.Difficulty.EASY)
                1 -> startGame(DifficultyManager.Difficulty.NORMAL)
                2 -> startGame(DifficultyManager.Difficulty.HARD)
                3 -> startGame(DifficultyManager.Difficulty.INSANE)
                4 -> {
                    // Back
                    isDifficultySelect = false
                    selectedIndex = 0
                }
            }
        } else if (isOptionsSelect) {
            val sm = game.soundManager
            when (index) {
                0 -> sm.soundEnabled = !sm.soundEnabled // Toggle Sound
                1 -> sm.musicEnabled = !sm.musicEnabled // Toggle Music
                2 -> { // Toggle Track
                    sm.currentTrack =
                            if (sm.currentTrack ==
                                            com.tappybird.systems.SoundManager.MusicTrack.WHAT
                            )
                                    com.tappybird.systems.SoundManager.MusicTrack.DARK_FOREST
                            else com.tappybird.systems.SoundManager.MusicTrack.WHAT
                }
                3 -> { // Back
                    isOptionsSelect = false
                    selectedIndex = 2 // Return to "Options"
                }
            }
        }
    }

    private fun startGame(difficulty: DifficultyManager.Difficulty) {
        game.screen = GameScreen(game, difficulty)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)
    }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        titleFont.dispose()
        menuFont.dispose()
        shapeRenderer.dispose()
    }
}
