# Performance Code Analysis & Suggestions

This document outlines recommended changes to improve the efficiency of **Typing Toucan**, specifically targeting low-end mobile devices.

## 1. Memory & Allocation (Reduce Garbage Collection)
Low-end devices struggle with frequent Garbage Collection (GC) pauses. Minimizing object creation in the `render` loop (60 times/second) is critical.

- [x] Re-use `Vector3` for Input Processing
    - **Location**: `GameScreen.kt`
    - **Issue**: `handlePauseMenuTouch` creates a new `Vector3` every time the screen is touched (via `camera.unproject`).
    - **Fix**: Create a `private val tempVec = Vector3()` class member and reuse it.

- [x] Eliminate Iterator Allocations in Update Loop
    - **Location**: `GameScreen.kt`
    - **Issue**: `necks.iterator()` (or the `for (neck in necks)` loop) may allocate an `ArrayIterator` object every frame.
    - **Fix**: Use an index-based loop:
        ```kotlin
        for (i in necks.size - 1 downTo 0) {
            val neck = necks[i]
            // ... logic ...
            if (shouldRemove) necks.removeIndex(i)
        }
        ```

- [x] Optimize String Allocations in Text Mode Rendering
    - **Location**: `GameScreen.kt` (lines ~980-1044, `drawLine` helper)
    - **Issue**: Extensive use of `substring` (`take`, `drop`), `replace`, and String interpolation inside the `render` method.
    - **Fix**: 
        1. Use `GlyphLayout.setText(font, text, start, end...)` to measure substrings without creating new String objects.
        2. Avoid `replace` inside the loop. Sanitize text once upon loading.
        3. Recalculate text strings only when input changes, not every frame.

## 2. Rendering Optimization
Reducing draw calls and state switches allows the GPU to work efficiently.

- [x] Use a Texture Atlas (instead of 30+ individual PNGs)
    - [x] Status: COMPLETE
    - [x] Benefit: Reduces draw calls (Batching) and texture swaps significantly.
    - [x] Effort: Moderate (4/10)

-   **[ ] Preload Ground Animation Textures**
    -   **Location**: `GameScreen.kt`
    -   **Issue**: `Texture(Gdx.files...)` is called in `init` bypassing `AssetManager`.
    -   **Fix**: Add these to `TypingToucanGame`'s queue.

## 3. General Logic Fixes

- [x] Remove Duplicate Audio Call
    - **Location**: `GameScreen.kt` (line 547)
    - **Issue**: `soundManager.playScore()` is called twice consecutively.
    - **Fix**: Remove one line.

## Plan of Action
1.  **Phase 1 (Quick Wins)**: Fix `Vector3`, Loop Iterators, and Duplicate Audio.
2.  **Phase 2 (Deep Code)**: Refactor `GameScreen` text rendering.
3.  **Phase 3 (Assets)**: Implement TextureAtlas.
