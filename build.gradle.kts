buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        val androidPluginVersion: String by project
        val kotlinVersion: String by project
        classpath("com.android.tools.build:gradle:$androidPluginVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    apply(plugin = "eclipse")
    apply(plugin = "idea")
    
    version = "1.0-SNAPSHOT"

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    }
}

subprojects {
    
}
