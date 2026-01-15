package com.typingtoucan.systems

data class PassageItem(val text: String, val metadata: String)

/**
 * Typing source that serves random passages sequentially.
 *
 * @param allPassages List of all available passages to cycle through.
 */
class TextSnippetSource(private val allPassages: List<PassageItem>, private val sequential: Boolean = false) : TypingSource {
    override fun setCapitalsEnabled(enabled: Boolean) {
        // No-op for text mode
    }

    // State
    private var currentIndex = 0 // Global read pointer across passages
    private var typedIndex = 0 // Tracks progress in CURRENT passage text
    // For sequential mode
    private var listIndex = 0
    
    // Formatting
    val displayLines = mutableListOf<String>()
    private var processedText: String = ""
    
    // Metadata
    // We track current passage and next passage info for seamless transition
    var sourceMetadata = ""
    private var nextSourceMetadata = ""
    private var nextDisplayLines = listOf<String>()
    private var nextProcessedText: String = ""
    
    // Initialize first passage
    init {
        if (allPassages.isEmpty()) throw IllegalArgumentException("Passages cannot be empty")
        // Load Initial
        if (sequential) {
            listIndex = 0
            setupCurrent(allPassages[0])
        } else {
            val first = allPassages.random()
            setupCurrent(first)
        }
        preloadNext()
    }
    
    // Helpers to process text
    private fun processPassage(p: PassageItem): Pair<List<String>, String> {
        val raw = p.text.replace('\n', ' ').filter { !it.isISOControl() }
        val cleanText = if (raw.isNotEmpty()) raw + " " else raw
        val lines = wordWrap(cleanText, 15)
        // Ensure processedText exactly matches join string (spaces included)
        // If lines are ["A", "B"], join is "A B". Space is implicit char.
        val text = lines.joinToString(" ")
        return Pair(lines, text)
    }

    private fun setupCurrent(p: PassageItem) {
        sourceMetadata = p.metadata
        val (lines, text) = processPassage(p)
        displayLines.clear()
        displayLines.addAll(lines)
        processedText = text
        
        currentIndex = 0
        typedIndex = 0
    }
    
    private fun preloadNext() {
        val p = if (sequential) {
            // Check if we just loaded final credit? Loop?
            // User says "autoplays... display credits". Usually loops or ends?
            // Infinite loop is safest for Text Mode architecture.
            val nextIdx = (listIndex + 1) % allPassages.size
            // Note: listIndex is tracking CURRENTLY PRELOADED next.
            // Wait. init calls setupCurrent (idx 0), then preloadNext.
            // preloadNext should load idx 1.
            // So if listIndex was used for CURRENT, we increment first.
            // If listIndex=0 (Current). Next is 1.
            // But I updated listIndex in init? No.
            // If sequential: listIndex=0. setupCurrent(0).
            // preloadNext logic needs to increment first.
            // BUT listIndex should track what is IN next? Or what was LAST used?
            // Let's assume listIndex tracks the one currently in 'processedText' (or 'nextProcessedText'?)
            // If init: listIndex=0. Used by Current.
            // preloadNext: listIndex becomes 1. Used by Next.
            // swap: Current becomes Next (idx 1).
            // preloadNext: listIndex becomes 2.
            // Matches.
            listIndex = (listIndex + 1) % allPassages.size
            allPassages[listIndex]
        } else {
            allPassages.random()
        }
        nextSourceMetadata = p.metadata
        val (lines, text) = processPassage(p)
        nextDisplayLines = lines
        nextProcessedText = text
    }

    private fun wordWrap(text: String, limit: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.length + word.length + (if (currentLine.isNotEmpty()) 1 else 0) <= limit) {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    override fun getNextChar(): Char {
        // Fetch from Current or Next
        // We do NOT swap passages here. Queue pre-fetches.
        
        if (currentIndex < processedText.length) {
            return processedText[currentIndex++]
        }
        
        // Reading from Next
        val nextIndex = currentIndex - processedText.length
        if (nextIndex < nextProcessedText.length) {
            currentIndex++
            return nextProcessedText[nextIndex]
        }
        
        // Next exhausted? Cycle (should be rare given update rate)
        // For robustness, could force a preload here, but simplest logic assumes swap happens eventually.
        return ' ' 
    }
    
    private fun performSwap() {
        // Current becomes Next
        sourceMetadata = nextSourceMetadata
        displayLines.clear()
        displayLines.addAll(nextDisplayLines)
        processedText = nextProcessedText
        
        // Rebase indices
        currentIndex = (currentIndex - processedText.length).coerceAtLeast(0) // Should be > 0 usually
        // Actually, if currentIndex was reading Next, we subtract OLD length? No.
        // We subtract the length of the passage we just finished (which WAS processedText).
        // BUT processedText is already updated? No wait.
        // Logic:
        // oldLen = processedText.length
        // processing...
        // processedText = nextProcessedText
        // currentIndex -= oldLen
        
        // Wait, 'processedText' variable is overwritten before calculation if I do lines above.
        // Correct Order:
        val oldLen = processedText.length
        
        sourceMetadata = nextSourceMetadata
        displayLines.clear()
        displayLines.addAll(nextDisplayLines)
        processedText = nextProcessedText
        
        // Rebase
        // Hard reset to 3 matches the TypingQueue buffer size.
        // This ensures that we are reading exactly from the 4th character of the NEW passage,
        // which corresponds to the fact that the queue already holds [Char0, Char1, Char2].
        currentIndex = 3
        typedIndex = 0 // Reset user progress for new passage
        
        // Preload new Next
        preloadNext()
    }

    override fun onCharTyped(char: Char) {
        typedIndex++
        
        // Check for Completion
        // If we finished typing current text (including trailing spaces/joins)
        if (typedIndex >= processedText.length) {
             performSwap()
        }
    }

    override fun onCrash(char: Char) {
        // No logic needed
    }
    
    // --- UI Helpers ---

    data class DisplayState(
        val currentLine: String,
        val nextLine: String,
        val prevLine: String,
        val localProgress: Int,
        val lineIndex: Int
    )

    /** 
     * Returns the full state including previous line for animation.
     */
    fun getDisplayState(): DisplayState {
        // Calculate which line matches 'typedIndex'
        var charCount = 0
        var lineIdx = 0
        
        for (i in displayLines.indices) {
            val len = displayLines[i].length + 1 // space join
            if (typedIndex < charCount + len) {
                lineIdx = i
                break
            }
            charCount += len
            lineIdx = i // Safety clamp
        }
        
        if (lineIdx >= displayLines.size && displayLines.isNotEmpty()) lineIdx = displayLines.size - 1

        val currentStr = displayLines.getOrElse(lineIdx) { "" }
        
        val nextStr = 
            if (lineIdx + 1 < displayLines.size) {
                displayLines[lineIdx + 1]
            } else {
                if (nextDisplayLines.isNotEmpty()) nextDisplayLines[0] else ""
            }
            
        val prevStr = if (lineIdx > 0) displayLines[lineIdx - 1] else ""
        
        // Local progress
        // Re-calculate start CharCount for this line strictly
        // We need exact start index of current line
        var startCharIdx = 0
        for (i in 0 until lineIdx) {
            startCharIdx += displayLines[i].length + 1
        }
        
        val localProgress = (typedIndex - startCharIdx).coerceIn(0, currentStr.length)
        
        return DisplayState(currentStr, nextStr, prevStr, localProgress, lineIdx)
    }

    override fun getProgressDisplay(): String {
        return "Inf" // Infinite mode
    }

    override fun isComplete(): Boolean {
        // Never complete in this mode
        return false
    }

    override fun expandPool(): List<Char> {
        return emptyList()
    }
}
