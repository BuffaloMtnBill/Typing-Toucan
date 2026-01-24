package com.typingtoucan.systems

/**
 * Interface defining a source of characters for the typing gameplay.
 */
interface TypingSource {
    /** Returns the next character to be added to the queue. */
    fun getNextChar(): Char

    /** Called when a character is successfully typed by the user. */
    fun onCharTyped(char: Char)

    /** Called when the bird crashes (optional penalty logic). */
    fun onCrash(char: Char)

    /** Returns current progress info (e.g. "Level 1" or "45/100"). */
    fun getProgressDisplay(): String

    /** Returns true if the content is fully exhausted/completed. */
    fun isComplete(): Boolean

    /**
     * Expands the pool of available characters (Classic mode).
     *
     * @return List of newly added characters, or empty list if not applicable.
     */
    fun expandPool(): List<Char>

    /** Toggles usage of capital letters (Classic mode mainly). */
    fun setCapitalsEnabled(enabled: Boolean)
}
