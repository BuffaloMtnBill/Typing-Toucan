package com.typingtoucan.systems

class TextSnippetSource(private val text: String) : TypingSource {
    override fun setCapitalsEnabled(enabled: Boolean) {
        // No-op for text mode
    }
    private var currentIndex = 0

    // Filter text to valid printable chars just in case?
    // Assuming text is pre-processed or valid.

    // We might need to handle newlines?
    // The game doesn't support newlines in the queue well (it's a horizontal line).
    // We should replace newlines with spaces or specialized symbols?
    private val processedText =
            text.replace('\n', ' ').filter { it.isDefined() && !it.isISOControl() }

    init {
        if (processedText.isEmpty()) throw IllegalArgumentException("Snippet cannot be empty")
    }

    override fun getNextChar(): Char {
        if (currentIndex < processedText.length) {
            val char = processedText[currentIndex]
            currentIndex++
            return char
        }
        return ' ' // Formatting spacer if exhausted before end logic catches it?
    }

    override fun onCharTyped(char: Char) {
        // No logic needed
    }

    override fun onCrash(char: Char) {
        // Rewind? Or just verify logic.
        // If we crash on a letter, we usually retry it because it's still in the queue (GameScreen
        // logic).
        // Since getNextChar advances index, if we crash, we don't need to do anything here
        // unless we want to "push back" index.
        // But the queue in TypingQueue acts as a buffer.
        // The letter stays in queue until typed.
    }

    override fun getProgressDisplay(): String {
        return "${currentIndex}/${processedText.length}"
    }

    override fun isComplete(): Boolean {
        return currentIndex >= processedText.length
    }

    override fun expandPool(): List<Char> {
        return emptyList()
    }
}
