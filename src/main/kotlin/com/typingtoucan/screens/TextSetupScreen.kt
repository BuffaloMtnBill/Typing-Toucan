package com.typingtoucan.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.typingtoucan.TypingToucanGame
import com.typingtoucan.systems.DifficultyManager
import com.typingtoucan.systems.TextSnippetSource

/**
 * Screen used to select a predefined text snippet for the "Text Mode" practice session.
 *
 * @param game The main game instance.
 */
class TextSetupScreen(val game: TypingToucanGame) : Screen {
    private val camera = OrthographicCamera().apply { setToOrtho(false, 800f, 600f) }
    private val viewport = com.badlogic.gdx.utils.viewport.ExtendViewport(800f, 600f, camera)

    private lateinit var uiFont: BitmapFont
    private val shapeRenderer = ShapeRenderer()
    private val layout = GlyphLayout()

    /** List of predefined text snippets available for selection. */
    private val snippets =
            listOf(
                    "Pangrams" to
                            "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs. How vexingly quick daft zebras jump!",
                    "Lorem Ipsum" to
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                    "Code Snippet" to
                            "fun main() { println(\"Hello, World!\") } val x = 10; if (x > 5) { return true } else { return false }",
                    "Short Story" to
                            "Once upon a time, in a digital forest, lived a small toucan who loved to type. Every keystroke made him fly higher."
            )

    private val snippetRects = mutableListOf<Rectangle>()
    private val backRect = Rectangle(50f, 50f, 150f, 50f)

    init {
        val generator =
                FreeTypeFontGenerator(Gdx.files.internal("assets/OriginalSurfer-Regular.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 20
        parameter.color = Color.WHITE
        parameter.borderColor = Color.BLACK
        parameter.borderWidth = 1f
        uiFont = generator.generateFont(parameter)
        generator.dispose()

        // Layout
        var y = 450f
        snippets.forEach { _ ->
            snippetRects.add(Rectangle(100f, y, 600f, 60f))
            y -= 80f
        }
    }

    override fun render(delta: Float) {
        handleInput()

        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        game.batch.projectionMatrix = camera.combined
        shapeRenderer.projectionMatrix = camera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Snippet Buttons
        val mx = Gdx.input.x
        val my = Gdx.input.y
        val touch = camera.unproject(Vector3(mx.toFloat(), my.toFloat(), 0f))

        snippetRects.forEachIndexed { index, rect ->
            if (rect.contains(touch.x, touch.y)) {
                shapeRenderer.color = Color.CYAN
            } else {
                shapeRenderer.color = Color.DARK_GRAY
            }
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
        }

        // Back Button
        shapeRenderer.color = Color.RED
        shapeRenderer.rect(backRect.x, backRect.y, backRect.width, backRect.height)

        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        game.batch.begin()

        // Title
        layout.setText(uiFont, "Select Text to Type")
        uiFont.draw(game.batch, "Select Text to Type", 400f - layout.width / 2, 550f)

        // Snippet Titles
        snippets.forEachIndexed { index, (title, _) ->
            val rect = snippetRects[index]
            layout.setText(uiFont, title)
            uiFont.draw(game.batch, title, rect.x + 20f, rect.y + 40f)

            // Preview?
            // uiFont.draw() ...
        }

        layout.setText(uiFont, "BACK")
        uiFont.draw(
                game.batch,
                "BACK",
                backRect.x + backRect.width / 2 - layout.width / 2,
                backRect.y + backRect.height / 2 + layout.height / 2
        )

        game.batch.end()
    }

    private fun handleInput() {
        if (Gdx.input.justTouched()) {
            val touch = camera.unproject(Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

            snippetRects.forEachIndexed { index, rect ->
                if (rect.contains(touch.x, touch.y)) {
                    val text = snippets[index].second
                    game.screen =
                            GameScreen(
                                    game,
                                    DifficultyManager.Difficulty.NORMAL,
                                    isPracticeMode = true,
                                    customSource = TextSnippetSource(text)
                            )
                }
            }

            if (backRect.contains(touch.x, touch.y)) {
                game.screen = MenuScreen(game)
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.screen = MenuScreen(game)
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        camera.position.set(viewport.worldWidth / 2f, viewport.worldHeight / 2f, 0f)
    }

    override fun show() {
        Gdx.input.inputProcessor = null
    }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        uiFont.dispose()
        shapeRenderer.dispose()
    }
}
