package com.typingtoucan.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.typingtoucan.TypingToucanGame
import com.typingtoucan.systems.DifficultyManager
import com.typingtoucan.utils.SaveManager

/**
 * The primary menu screen of the game.
 *
 * Handles navigation between different game modes (Start Game, Custom Mode, Text Mode),
 * options configuration, and viewing credits.
 *
 * @param game The main game instance.
 */
class MenuScreen(val game: TypingToucanGame) : Screen {
    private val stage = Stage(ScreenViewport())
    private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 600f) }
    private val viewport = com.badlogic.gdx.utils.viewport.ExtendViewport(800f, 600f, camera)
    private val tempVec = com.badlogic.gdx.math.Vector3()

    /** List of main menu options. */
    private val options = listOf("Learn to Type", "Practice", "Arcade", "Text Typing", "Options", "Credits")
    /** List of descriptions corresponding to main menu options. */
    private val descriptions =
            listOf(
                    "Standard progression mode. Unlock keys as you go.",
                    "Practice specific keys with no penalties.",
                    "All letters unlocked, get the longest streak!",
                    "Type full paragraphs and stories.",
                    "Adjust sound and music settings.",
                    "View the game credits."
            )
    
    // Assets
    private lateinit var titleFont: BitmapFont
    private lateinit var menuFont: BitmapFont
    private lateinit var captionFont: BitmapFont
    private val shapeRenderer = ShapeRenderer()
    private val layout = GlyphLayout()

    
    /** List of difficulty levels available for selection. */
    private val difficultyOptions = listOf("Easy", "Normal", "Hard", "Insane", "", "Change Start Level")
    
    /** List of settings available in the options submenu. */
    private val optionsMenuItems = listOf("Sound", "Music", "Music Track", "Reset High Score", "Back")
    
    /** Index of the currently selected menu item. */
    private var selectedIndex = 0
    private var isDifficultySelect = false
    private var isTextModeSelect = false
    private var isArcadeModeSelect = false
    private var isOptionsSelect = false
    private var startLevel = 1
    private val progressionString = "asdfjkl;ghqweruioptyzxcvm,./bn1234567890-!@#$%()"
    private val SELECTED_COLOR = Color(1f, 0.906f, 0f, 1f) // #ffe700
    
    // ... (skipping unchanged code) ...

    /** ... */
    // (Inside selectOption method, explicitly targeting the isOptionsSelect block)
    // I need to be careful with replace_file_content context.
    // I will replace the optionsMenuItems definition and the isOptionsSelect block in selectOption.
    // Wait, I can't do two disjoint replacements easily if they are far apart unless I use multiple chunks.
    // Let's use multiple chunks.


    init {
        // Initialize Fonts
        val generator =
                FreeTypeFontGenerator(Gdx.files.internal("assets/OriginalSurfer-Regular.ttf"))

        val titleParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        titleParam.size = 70 // Increased for mobile visibility
        titleParam.color = Color.GOLD
        titleParam.borderColor = Color.BLACK
        titleParam.borderWidth = 3f
        titleFont = generator.generateFont(titleParam)

        val menuParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        menuParam.size = 35 // Increased for mobile visibility
        menuParam.color = Color.WHITE
        menuParam.borderColor = Color.BLACK
        menuParam.borderWidth = 2f
        menuFont = generator.generateFont(menuParam)

        val captionParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        captionParam.size = 24 // Increased for mobile visibility
        captionParam.color = Color.LIGHT_GRAY
        captionParam.borderColor = Color.BLACK
        captionParam.borderWidth = 1f
        captionFont = generator.generateFont(captionParam)

        generator.dispose()
    }

    override fun show() {
        // Reset selection on show
        selectedIndex = 0
        isDifficultySelect = false
        isOptionsSelect = false
        isArcadeModeSelect = false

        // Start Menu Music
        if (game.soundManager.musicEnabled) {
            game.soundManager.currentTrack =
                    com.typingtoucan.systems.SoundManager.MusicTrack.DARK_FOREST
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
        val topY = viewport.worldHeight - 70f // Increased padding from top

        layout.setText(titleFont, "TYPING TOUCAN")
        titleFont.draw(game.batch, "TYPING TOUCAN", centerX - layout.width / 2, topY)

        // High Score
        // High Score
        // Show High Score based on selected option
        var scoreText = ""
        if (!isDifficultySelect && !isOptionsSelect) {
            scoreText = when (selectedIndex) {
                0 -> "Most Levels Unlocked: ${SaveManager.getNormalLevel()}"
                1 -> "Best Streak: ${SaveManager.getCustomStreak()}"
                2 -> "Best Streak: ${SaveManager.getArcadeStreak()}" // Arcade Mode
                3 -> "Best Streak: ${SaveManager.getTextStreak()}"
                else -> ""
            }
        }

        if (scoreText.isNotEmpty()) {
            layout.setText(menuFont, scoreText)
            menuFont.draw(game.batch, scoreText, centerX - layout.width / 2, topY - 70f)
        }

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
        val centerY = viewport.worldHeight / 2f + 95f // Shifted down 1/2 line (120 - 25)
        var startY = centerY
        val gap = 50f

        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            menuFont.color = if (isSelected) SELECTED_COLOR else Color.WHITE

            layout.setText(menuFont, option)
            val x = centerX - layout.width / 2
            val y = startY - (index * gap)

            menuFont.draw(game.batch, option, x, y)

            if (isSelected) {
                // No underline
            }
        }
        // Reset color
        menuFont.color = Color.WHITE

        // Draw Caption
        if (selectedIndex in descriptions.indices) {
            val caption = descriptions[selectedIndex]
            layout.setText(captionFont, caption)
            captionFont.draw(game.batch, caption, centerX - layout.width / 2, 80f) // Increased padding from bottom
        }
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
            if (option.isEmpty()) return@forEachIndexed // Skip blank lines
            
            val isSelected = index == selectedIndex
            menuFont.color = if (isSelected) SELECTED_COLOR else Color.WHITE

            var displayOption = option
            
            // Hide "Change Start Level" for Text Mode OR Arcade Mode
            if ((isTextModeSelect || isArcadeModeSelect) && index == 5) {
                 return@forEachIndexed // Skip drawing "Change Start Level"
            }

            if (index == 5) { // Change Start Level
                 val char = if (startLevel <= progressionString.length) progressionString[startLevel - 1] else '?'
                 displayOption = "< Change Start Level: $startLevel - $char >"
            }

            layout.setText(menuFont, displayOption)
            val x = centerX - layout.width / 2
            val y = startY - (index * gap)

            menuFont.draw(game.batch, displayOption, x, y) // Use displayOption

            if (isSelected) {
                // No underline
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
                                    if (game.soundManager.pendingTrack ==
                                                    com.typingtoucan.systems.SoundManager.MusicTrack
                                                            .WHAT
                                    )
                                            "What"
                                    else "Dark Forest"
                            "Music Track: $trackName"
                        }
                        "Reset High Score" -> "Reset High Scores"
                        else -> item
                    }

            val isSelected = index == selectedIndex
            menuFont.color = if (isSelected) SELECTED_COLOR else Color.WHITE

            layout.setText(menuFont, label)
            val x = centerX - layout.width / 2
            val y = startY - (index * gap)

            menuFont.draw(game.batch, label, x, y)

            if (isSelected) {
                // No underline
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

        // --- Touch / Mouse Handling ---
        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.x.toFloat()
            val touchY = Gdx.input.y.toFloat()
            // Unproject to world coordinates using reusable vector
            tempVec.set(touchX, touchY, 0f)
            val worldPos = camera.unproject(tempVec)

            val centerX = viewport.worldWidth / 2f
            val centerY = viewport.worldHeight / 2f + 50f
            var startY = centerY
            val gap = 50f

            // Iterate through visible options to check click bounds
            // We use the same layout loop logic as drawing to determine hitboxes
            currentList.forEachIndexed { index, option ->
                // Skip "Change Start Level" for Text Mode or Arcade Mode if it's not drawn
                if ((isTextModeSelect || isArcadeModeSelect) && isDifficultySelect && index == 5) {
                    return@forEachIndexed
                }

                layout.setText(menuFont, option)
                val w = layout.width
                val h = layout.height
                val x = centerX - w / 2
                val y = startY - (index * gap)

                // Define a generous hitbox around the text
                // y is the baseline, so hitbox goes from y to y+h
                // padding of 20px
                if (worldPos.x >= x - 20 && worldPos.x <= x + w + 20 &&
                    worldPos.y >= y - h - 10 && worldPos.y <= y + 20) {
                    
                    if (isDifficultySelect && index == 5) {
                         // Touch Zones for Arrows
                         val rx = worldPos.x - x
                         if (rx < w * 0.25f) { // Left 25%
                             startLevel = (startLevel - 1).coerceAtLeast(1)
                         } else if (rx > w * 0.75f) { // Right 25%
                             startLevel = (startLevel + 1).coerceAtMost(progressionString.length)
                         }
                         selectedIndex = index
                         // Optional: Add simple debounce or wait for release if needed, but simple tap works for now
                         return
                    }

                    selectedIndex = index
                    // Single tap execute (unless it's the slider which is handled above)
                    selectOption(index)
                    return // Stop checking
                }
            }
        }

        // --- Keyboard Navigation ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex--
            if (selectedIndex < 0) selectedIndex = currentList.size - 1
            // Skip empty items (separators)
            if (currentList[selectedIndex].isEmpty()) selectedIndex--
            if (selectedIndex < 0) selectedIndex = currentList.size - 1

            // Skip "Change Start Level" (index 5) in Text Mode or Arcade Mode
            if ((isTextModeSelect || isArcadeModeSelect) && isDifficultySelect && selectedIndex == 5) {
                selectedIndex = 4 // Skip to the item above
                if (selectedIndex < 0) selectedIndex = currentList.size - 1
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex++
            if (selectedIndex >= currentList.size) selectedIndex = 0
            
            // Skip "Change Start Level" (index 5) in Text Mode or Arcade Mode
            if ((isTextModeSelect || isArcadeModeSelect) && isDifficultySelect && selectedIndex == 5) {
                selectedIndex = 0 // Wrap to top
            }

            // Skip empty items (separators)
            if (currentList[selectedIndex].isEmpty()) selectedIndex++
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            if (isDifficultySelect) {
                isDifficultySelect = false
                isTextModeSelect = false
                isArcadeModeSelect = false
                selectedIndex = 0
            } else if (isOptionsSelect) {
                isOptionsSelect = false
                selectedIndex = 0
            } else {
                Gdx.app.exit()
            }
        }

        // Left / Right for Slider Options
        if (isDifficultySelect && selectedIndex == 5) {
             if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                 startLevel = (startLevel + 1).coerceAtMost(progressionString.length)
             }
             if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                 startLevel = (startLevel - 1).coerceAtLeast(1)
             }
        }
    }

    private fun selectOption(index: Int) {
        // Play Selection Sound
        game.soundManager.playMenuSelect()

        if (!isDifficultySelect && !isOptionsSelect) {
            // Main Menu Selection
            val selectedOption = options[index]
            when (selectedOption) {
                "Learn to Type" -> {
                    // Learning Mode
                    isDifficultySelect = true
                    selectedIndex = 1 // Default to Normal
                }
                "Practice" -> {
                    // Practice Mode
                    game.screen = CustomSetupScreen(game)
                }
                "Arcade" -> {
                    isDifficultySelect = true
                    isArcadeModeSelect = true
                    selectedIndex = 1 // Default to Normal
                }
                "Text Typing" -> {
                    // Text Mode
                    isDifficultySelect = true
                    isTextModeSelect = true
                    selectedIndex = 1 // Default to Normal
                }
                "Options" -> {
                    // Options
                    isOptionsSelect = true
                    selectedIndex = 0
                }
                "Credits" -> {
                    // Credits
                    try {
                        val file = Gdx.files.internal("assets/credits.txt")
                        val text = file.readString()
                        // Credits file is newline separated
                        // Keep empty lines to allow for scrolling spacing
                        val lines = text.split("\n")
                        val items = lines.map { com.typingtoucan.systems.PassageItem(it.trim(), "") }
                        val src = com.typingtoucan.systems.TextSnippetSource(items, sequential = true)

                        game.screen = GameScreen(
                                game,
                                DifficultyManager.Difficulty.INSANE,
                                isPracticeMode = true,
                                customSource = src,
                                isAutoplay = true
                        )
                    } catch (e: Exception) {
                        Gdx.app.error("MenuScreen", "Failed to load credits", e)
                    }
                }
            }
        } else if (isDifficultySelect) {
            // Difficulty Selection
            // Difficulty Selection
            if (isArcadeModeSelect) {
                val selectedDifficulty = when (index) {
                    0 -> DifficultyManager.Difficulty.EASY
                    1 -> DifficultyManager.Difficulty.NORMAL
                    2 -> DifficultyManager.Difficulty.HARD
                    3 -> DifficultyManager.Difficulty.INSANE
                    else -> DifficultyManager.Difficulty.NORMAL
                }
                
                // Arcade Mode Launch
                val allChars = progressionString.toList()
                val source = com.typingtoucan.systems.CustomPoolSource(allChars)
                
                game.screen =
                        GameScreen(
                                game,
                                difficulty = selectedDifficulty,
                                isPracticeMode = false,
                                customSource = source,
                                isArcadeMode = true
                        )
            } else if (isTextModeSelect) {
                when (index) {
                     0 -> game.screen = TextSetupScreen(game, DifficultyManager.Difficulty.EASY)
                     1 -> game.screen = TextSetupScreen(game, DifficultyManager.Difficulty.NORMAL)
                     2 -> game.screen = TextSetupScreen(game, DifficultyManager.Difficulty.HARD)
                     3 -> game.screen = TextSetupScreen(game, DifficultyManager.Difficulty.INSANE)
                }
            } else {
                when (index) {
                    0 -> startGame(DifficultyManager.Difficulty.EASY)
                    1 -> startGame(DifficultyManager.Difficulty.NORMAL)
                    2 -> startGame(DifficultyManager.Difficulty.HARD)
                    3 -> startGame(DifficultyManager.Difficulty.INSANE)
                    4 -> { /* Separator - Should be skipped */ }
                    5 -> { /* Change Start Level (Left/Right) */ }
                }
            }
        } else if (isOptionsSelect) {
            val sm = game.soundManager
            when (index) {
                0 -> sm.soundEnabled = !sm.soundEnabled // Toggle Sound
                1 -> sm.musicEnabled = !sm.musicEnabled // Toggle Music
                2 -> { // Toggle Track
                    sm.pendingTrack =
                            if (sm.pendingTrack ==
                                            com.typingtoucan.systems.SoundManager.MusicTrack.WHAT
                            )
                                    com.typingtoucan.systems.SoundManager.MusicTrack.DARK_FOREST
                            else com.typingtoucan.systems.SoundManager.MusicTrack.WHAT
                }
                3 -> {
                    // Reset High Score
                    SaveManager.resetHighScore()
                    game.soundManager.playLevelUpPractice() // Feedback sound
                }
                4 -> { // Back
                    isOptionsSelect = false
                    selectedIndex = 3 // Return to "Options"
                }
            }
        }
    }

    private fun startGame(difficulty: DifficultyManager.Difficulty) {
        game.screen = GameScreen(game, difficulty, startLevel = startLevel)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)
    }
    override fun pause() {
        game.soundManager.pauseMusic()
    }
    override fun resume() {
        game.soundManager.resumeMusic()
    }
    override fun hide() {}
    override fun dispose() {
        titleFont.dispose()
        menuFont.dispose()
        captionFont.dispose()
        shapeRenderer.dispose()
    }
}
