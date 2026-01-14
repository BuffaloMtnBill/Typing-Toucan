package com.typingtoucan.systems

class CustomPoolSource(private val characters: List<Char>) : TypingSource {

    override fun setCapitalsEnabled(enabled: Boolean) {
        // No-op for custom pool
    }

    init {
        if (characters.isEmpty()) throw IllegalArgumentException("Custom pool cannot be empty")
    }

    override fun getNextChar(): Char {
        return characters.random()
    }

    override fun onCharTyped(char: Char) {
        // No weighting in basic custom mode
    }

    override fun onCrash(char: Char) {
        // No penalty in basic custom mode
    }

    override fun getProgressDisplay(): String {
        return "Practice"
    }

    override fun isComplete(): Boolean {
        return false // Endless
    }

    override fun expandPool(): List<Char> {
        return emptyList() // No expansion
    }
}
