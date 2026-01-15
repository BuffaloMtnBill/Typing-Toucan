plugins {
    kotlin("jvm") version "2.1.0"
    application
}

group = "com.typingtoucan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

val gdxVersion = "1.13.0"

dependencies {
    implementation(kotlin("stdlib"))
    
    // LibGDX Core
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    
    // LibGDX Desktop (LWJGL3)
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    
    // LibGDX Box2D
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")
    
    // LibGDX Freetype
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

application {
    mainClass.set("com.typingtoucan.DesktopLauncherKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}
