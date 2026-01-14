package com.typingtoucan.systems

class TypingQueue(private val source: TypingSource) {

    val queue = mutableListOf<Char>()
    val queueSize = 3

    init {
        repeat(queueSize) { addLetter() }
    }

    private fun addLetter() {
        if (!source.isComplete() || hasBufferedChars()) {
            try {
                // Defensive check if source might throw or return dummy if complete
                // TextSource returns ' ' or similar if exhausted but checked via isComplete
                if (!source.isComplete()) {
                    queue.add(source.getNextChar())
                }
            } catch (e: Exception) {
                // Log?
            }
        }
    }

    // Helper to check if we can add more (hacky for TextSource end)
    private fun hasBufferedChars() = false // Logic managed by source state

    fun handleInput(char: Char, updateWeights: Boolean = true): Int? {
        if (queue.isEmpty()) {
            repeat(queueSize) { addLetter() }
        }

        if (queue.isNotEmpty() && queue.first() == char) {
            val matchedChar = queue.removeAt(0)

            if (updateWeights) {
                source.onCharTyped(matchedChar)
            }

            addLetter()

            // Return logic for weights?
            // ClassicSource has weights. Others don't.
            // We can return 0 or fetch from source if we add `getWeight(char)` to interface?
            // Existing code expects an Int return (weight) for the flash effect.
            // Let's assume 0 if not supported or change return type.
            // For now, return 5 (mid) if generic? Or 0.
            return 5
        }
        return null
    }

    fun onCrash(crashedChar: Char) {
        source.onCrash(crashedChar)
    }

    // Proxy methods for GameScreen compatibility
    fun expandPool(): List<Char> = source.expandPool()
    fun isFullyUnlocked(): Boolean = source.isComplete()

    fun setCapitalsEnabled(enabled: Boolean) {
        source.setCapitalsEnabled(enabled)
    }

    // For legacy/Classic compatibility - helpers
    // Not ideally clean but practical
    fun getSource() = source
}
