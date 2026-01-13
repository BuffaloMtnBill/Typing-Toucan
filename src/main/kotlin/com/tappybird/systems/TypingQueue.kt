package com.tappybird.systems

class TypingQueue {
    // Full Character Set (Lower -> Upper -> Numbers)
    // "probabilistic table with all letters, upper case letters and numbers"
    // We should probably define a logical progression order.
    // Preserving the existing "standard" progression for home row but expanding.
    // Let's append Upper and Numbers to the end for now.
    // Progression Stages
    private val progressionStages =
            listOf(
                    "asdfjkl;ghqweruioptyzxcvm,./bn1234567890-", // Main Sequence
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ", // Capitals (Bulk)
                    "!@#$%()" // Specials
            )

    private var currentStageIndex = 0
    private var currentStageCharIndex = 0

    // Flattened list for convenience if needed, but we manage activePool manually now.
    // private val standardLetters ... removed in favor of dynamic stages

    private var activePool = mutableListOf<Char>()
    val queue = mutableListOf<Char>()
    val queueSize = 3

    // Weights Map (Char -> Weight)
    private val weights = mutableMapOf<Char, Int>().withDefault { 0 }

    private val vowels = setOf('a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U')

    init {
        // Initialize with the first character of the first stage
        val firstChar = progressionStages[0][0]
        activePool.add(firstChar)
        weights[firstChar] = 10
        currentStageCharIndex = 1 // Next char to add from stage 0

        repeat(queueSize) { addLetter() }
    }

    fun addLetter() {
        // "The next letter... is selected randomly by lottery... points representing a possible
        // pick"
        // Only select from activePool
        val pool = activePool

        // Calculate total weight of active pool
        var totalWeight = 0
        pool.forEach { char ->
            val w = weights.getValue(char)
            // Ensure min weight of 1 for active letters if they somehow act up,
            // though the rules say min 1/5.
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

        queue.add(selectedChar)
    }

    fun handleInput(char: Char, updateWeights: Boolean = true): Int? {
        if (queue.isEmpty()) {
            repeat(queueSize) { addLetter() }
        }

        if (queue.isNotEmpty() && queue.first() == char) {
            val matchedChar = queue.removeAt(0)

            var currentWeight = weights.getValue(matchedChar)

            if (updateWeights) {
                val minWeight = if (vowels.contains(matchedChar)) 5 else 1

                if (currentWeight > minWeight) {
                    currentWeight -= 1
                    weights[matchedChar] = currentWeight
                }
            }

            addLetter()
            return currentWeight
        }
        return null
    }

    fun onCrash(crashedChar: Char) {
        // "If the bird crashes into the ground, the letter at the front of the queue is set to 10
        // points."
        // The "front of the queue" is the one the user was TRYING to type (queue.first()).
        // The user might pass the char from GameScreen, or we can look at queue.first() if valid.
        // We will trust the argument or look at queue logic in GameScreen.
        // Assuming crashedChar is valid.
        weights[crashedChar] = 10
    }

    // Modified to return list of added characters
    fun expandPool(): List<Char> {
        val addedChars = mutableListOf<Char>()

        if (currentStageIndex >= progressionStages.size) return emptyList()

        val currentStageStr = progressionStages[currentStageIndex]

        // Check if we have more chars in current stage
        if (currentStageCharIndex < currentStageStr.length) {
            // Add next char from current stage
            // Special case: Capitals stage should be bulk added?
            // User requested: "unlocking all capital letters at the same time"
            // My stages: Index 5 is Capitals.

            if (currentStageIndex == 1) { // Capitals Stage
                // Add ALL remaining chars in this stage
                while (currentStageCharIndex < currentStageStr.length) {
                    val nextChar = currentStageStr[currentStageCharIndex]
                    activePool.add(nextChar)
                    weights[nextChar] = 10
                    addedChars.add(nextChar)
                    currentStageCharIndex++
                }
                currentStageIndex++ // Done with this stage
                currentStageCharIndex = 0
            } else {
                // Normal single char unlock
                val nextChar = currentStageStr[currentStageCharIndex]
                activePool.add(nextChar)
                weights[nextChar] = 10
                addedChars.add(nextChar)
                currentStageCharIndex++

                // If finished stage, prep for next
                if (currentStageCharIndex >= currentStageStr.length) {
                    currentStageIndex++
                    currentStageCharIndex = 0
                }
            }
        } else {
            // Should verify logic flow, but condition above handles transition.
            // If we are here, it means currentStageCharIndex was already at end, so we should have
            // incremented stage index.
            // Check if next stage exists
            if (currentStageIndex < progressionStages.size) {
                // Recursively call? Or just handle iteration.
                // The logic above increments stage index eagerly when finishing a stage.
                // So we should be pointing to a fresh stage or be out of bounds.
                // Re-evaluate
                return expandPool()
            }
        }

        return addedChars
    }

    fun shrinkPool() {
        if (activePool.size > 1) {
            val removedChar = activePool.removeAt(activePool.lastIndex)
            weights[removedChar] = 0 // Reset? Or keep memory? "starts off with 0".
        }
    }

    fun getLastUnlockedLetter(): Char = activePool.last()

    fun getNextUnlockLetter(): Char? {
        if (currentStageIndex >= progressionStages.size) return null
        val s = progressionStages[currentStageIndex]
        // Check boundary
        if (currentStageCharIndex < s.length) {
            return s[currentStageCharIndex]
        }
        // If at end of stage, check next stage?
        // Logic in expandPool handles stage transition. Here we just peek.
        // If currentStageCharIndex is at end, it means we are waiting for expandPool to be called
        // to jump to next stage?
        // Or we should peek next stage.
        if (currentStageCharIndex >= s.length) {
            if (currentStageIndex + 1 < progressionStages.size) {
                return progressionStages[currentStageIndex + 1][0]
            }
        }
        return null
    }

    fun isFullyUnlocked(): Boolean {
        return currentStageIndex >= progressionStages.size
    }
}
