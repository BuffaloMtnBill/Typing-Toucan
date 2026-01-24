package com.typingtoucan.utils

import com.badlogic.gdx.tools.texturepacker.TexturePacker

fun main() {
    try {
        val inputDir = "src/main/resources/assets"
        val rawDir = "src/main/resources/assets-raw"
        val outputDir = "src/main/resources/assets/atlas"
        
        // Prepare raw directory
        val rawFolder = java.io.File(rawDir)
        if (rawFolder.exists()) rawFolder.deleteRecursively()
        rawFolder.mkdirs()
        
        val assetsFolder = java.io.File(inputDir)
        val excludes = listOf("background_panoramic.png", "title_background.png", "victory_background.png", "oldground.png")
        
        println("Filtering assets for packing...")
        assetsFolder.listFiles()?.forEach { file ->
            if (file.isFile && file.extension.lowercase() == "png" && !excludes.contains(file.name)) {
                file.copyTo(java.io.File(rawFolder, file.name))
            }
        }
        
        val settings = TexturePacker.Settings()
        settings.maxWidth = 2048
        settings.maxHeight = 2048
        settings.edgePadding = true
        settings.duplicatePadding = true
        settings.filterMin = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        settings.filterMag = com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest
        
        // Ensure output dir exists
        java.io.File(outputDir).mkdirs()

        println("Packing textures from $rawDir to $outputDir...")
        TexturePacker.process(
            settings,
            rawDir,
            outputDir,
            "game"
        )
        
        // Clean up
        rawFolder.deleteRecursively()
        println("Texture packing complete! Raw files cleaned up.")
    } catch (e: Exception) {
        e.printStackTrace()
        System.exit(1)
    }
}
