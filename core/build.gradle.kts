plugins {
    kotlin("jvm")
    `java-library`
}

val gdxVersion: String by project

dependencies {
    api(kotlin("stdlib"))
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
