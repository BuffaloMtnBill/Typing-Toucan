package com.typingtoucan.systems

/**
 * Manages the queue of upcoming characters for the player to type.
 *
 * It buffers characters from a [TypingSource] and provides methods to validate input.
 *
 * @param source The source strategy for character generation (e.g. Classic, Custom, Text).
 */
class TypingQueue(private val source: TypingSource) {

    /** The current queue of visible characters. */
    val queue = mutableListOf<Char>()
    /** The target number of characters to keep in the queue. */
    val queueSize = 3

    init {
        repeat(queueSize) { addLetter() }
    }

    /** Adds a letter from the source to the queue if available. */
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

    /**
     * Processes a typed character input.
     *
     * If the input matches the head of the queue, it removes it, updates source weights,
     * and adds a new letter to the queue.
     *
     * @param char The character typed by the user.
     * @param updateWeights Whether to report this type event to the source (for weighting).
     * @return An integer weight/score for value if match was successful, null otherwise.
     */
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

    /** Delegates crash reporting to the source. */
    fun onCrash(crashedChar: Char) {
        source.onCrash(crashedChar)
    }

    // Proxy methods for GameScreen compatibility
    
    /** Proxies [TypingSource.expandPool]. */
    fun expandPool(): List<Char> = source.expandPool()
    /** Proxies [TypingSource.isComplete]. */
    fun isFullyUnlocked(): Boolean = source.isComplete()

    /** Proxies [TypingSource.setCapitalsEnabled]. */
    fun setCapitalsEnabled(enabled: Boolean) {
        source.setCapitalsEnabled(enabled)
    }

    // For legacy/Classic compatibility - helpers
    // Not ideally clean but practical
    fun getSource() = source
}
