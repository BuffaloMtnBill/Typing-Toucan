plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion: String by project

android {
    namespace = "com.typingtoucan.android"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.typingtoucan.android"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            // These should be populated in a local.properties or environment variables for security
            storeFile = file("release.keystore")
            storePassword = System.getenv("RELEASE_STORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
            assets.srcDirs("assets")
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val natives = configurations.create("natives")

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    
    add("natives", "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    add("natives", "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    add("natives", "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    add("natives", "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    
    add("natives", "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-armeabi-v7a")
    add("natives", "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-arm64-v8a")
    add("natives", "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86")
    add("natives", "com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86_64")
    
    add("natives", "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    add("natives", "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    add("natives", "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86")
    add("natives", "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
}

tasks.register("copyAndroidNatives") {
    doFirst {
        file("libs/armeabi-v7a").mkdirs()
        file("libs/arm64-v8a").mkdirs()
        file("libs/x86").mkdirs()
        file("libs/x86_64").mkdirs()

        natives.files.forEach { jar ->
            var outputDir: java.io.File? = null
            if (jar.name.endsWith("natives-armeabi-v7a.jar")) outputDir = file("libs/armeabi-v7a")
            if (jar.name.endsWith("natives-arm64-v8a.jar")) outputDir = file("libs/arm64-v8a")
            if (jar.name.endsWith("natives-x86.jar")) outputDir = file("libs/x86")
            if (jar.name.endsWith("natives-x86_64.jar")) outputDir = file("libs/x86_64")

            if (outputDir != null) {
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("*.so")
                }
            }
        }
    }
}

tasks.whenTaskAdded {
    if (name.contains("package")) {
        dependsOn("copyAndroidNatives")
    }
}
