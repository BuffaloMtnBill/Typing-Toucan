package com.typingtoucan.systems

/**
 * Standard game mode source where characters are unlocked progressively.
 *
 * Managing stages of character unlocks (from home row outward) and calculates
 * weighted random selection to prioritize new or struggling keys.
 */
class ClassicSource : TypingSource {
    /** Stages of character sets to unlock progressively. */
    private val progressionStages =
            listOf(
                    "asdfjkl;ghqweruioptyzxcvm,./bn1234567890-", // Main Sequence
                    // Capitals removed as separate stage
                    "!@#$%()" // Specials
            )

    private var currentStageIndex = 0
    private var currentStageCharIndex = 0

    // Source of truth for what has been unlocked via progression
    private val unlockedBaseChars = mutableListOf<Char>()
    private var activePool = mutableListOf<Char>()

    private var capitalsEnabled = false

    // Weights Map (Char -> Weight)
    private val weights = mutableMapOf<Char, Int>().withDefault { 0 }
    private val vowels = setOf('a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U')

    init {
        // Initialize with the first character of the first stage
        val firstChar = progressionStages[0][0]
        unlockedBaseChars.add(firstChar)
        weights[firstChar] = 10
        currentStageCharIndex = 1 // Next char to add from stage 0
        rebuildActivePool()
    }

    override fun setCapitalsEnabled(enabled: Boolean) {
        if (capitalsEnabled != enabled) {
            capitalsEnabled = enabled
            rebuildActivePool()
        }
    }

    private fun rebuildActivePool() {
        activePool.clear()
        unlockedBaseChars.forEach { char ->
            activePool.add(char)
            // Ensure weight exists
            if (!weights.containsKey(char)) weights[char] = 10

            if (capitalsEnabled && char.isLetter() && char.isLowerCase()) {
                val upper = char.uppercaseChar()
                activePool.add(upper)
                if (!weights.containsKey(upper)) weights[upper] = 10
            }
        }
    }

    override fun getNextChar(): Char {
        // Only select from activePool
        val pool = activePool
        if (pool.isEmpty()) return 'a' // Fallback safety

        // Calculate total weight of active pool
        var totalWeight = 0
        pool.forEach { char ->
            val w = weights.getValue(char)
            totalWeight += if (w > 0) w else 1
        }

        var randomValue = kotlin.random.Random.nextInt(totalWeight)
        var selectedChar = pool.last() // Fallback

        for (char in pool) {
            val w = weights.getValue(char)
            val effectiveWeight = if (w > 0) w else 1
            randomValue -= effectiveWeight
            if (randomValue < 0) {
                selectedChar = char
                break
            }
        }
        return selectedChar
    }

    override fun onCharTyped(char: Char) {
        var currentWeight = weights.getValue(char)
        val minWeight = if (vowels.contains(char)) 5 else 1

        if (currentWeight > minWeight) {
            currentWeight -= 1
            weights[char] = currentWeight
        }
    }

    override fun onCrash(char: Char) {
        weights[char] = 10
    }

    /**
     * Unlocks the next character from the progression stages.
     *
     * @return List of newly added characters.
     */
    override fun expandPool(): List<Char> {
        val addedChars = mutableListOf<Char>()

        if (currentStageIndex >= progressionStages.size) return emptyList()

        val currentStageStr = progressionStages[currentStageIndex]

        if (currentStageCharIndex < currentStageStr.length) {
            // Normal single char unlock
            val nextChar = currentStageStr[currentStageCharIndex]

            unlockedBaseChars.add(nextChar)
            weights[nextChar] = 10

            // Rebuild pool to include potential capital
            rebuildActivePool()

            // We report what was added.
            // If capitals enabled, we implicitly added the capital too,
            // but the "new" thing is the base char.
            // GameScreen might flash the char.
            addedChars.add(nextChar)
            if (capitalsEnabled && nextChar.isLetter() && nextChar.isLowerCase()) {
                addedChars.add(nextChar.uppercaseChar())
            }

            currentStageCharIndex++

            // If finished stage, prep for next
            if (currentStageCharIndex >= currentStageStr.length) {
                currentStageIndex++
                currentStageCharIndex = 0
            }
        }
        // Logic for transitioning stages is handled by index checks above

        return addedChars
    }

    /** Removes the most recently unlocked character (debugging tool). */
    fun shrinkPool() {
        if (unlockedBaseChars.size > 1) {
            unlockedBaseChars.removeAt(unlockedBaseChars.lastIndex)
            rebuildActivePool()
        }
    }

    override fun getProgressDisplay(): String {
        // Level is roughly determined by pool size?
        // Or we can return the current stage/char info.
        // For now, let's return just empty string as Level is managed by GameScreen counters
        // But the architecture plan said "Level 1".
        // Current GameScreen manages 'level' variable.
        // We can expose iteration info here if we want to move logic.
        // For now, keep it simple.
        return ""
    }

    override fun isComplete(): Boolean {
        return currentStageIndex >= progressionStages.size
    }

    /** For compatibility with GameScreen's "isFullyUnlocked". */
    fun isFullyUnlocked() = isComplete()
}
