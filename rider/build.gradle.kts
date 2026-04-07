plugins {
    id("org.jetbrains.intellij.platform") version "2.13.1"
    kotlin("jvm") version "2.3.20"
}

group = "com.igamesx.pasteenhance"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Use the locally installed Rider — no SDK download required
        local("D:/JetBrains Rider 2026.1")
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Paste Enhance"
        version = "1.0.0"
        ideaVersion {
            sinceBuild = "261"
            untilBuild = "261.*"
        }
    }
    buildSearchableOptions = false
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
