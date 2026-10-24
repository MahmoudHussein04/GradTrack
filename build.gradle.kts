buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 bundles its own (older) Kotlin compiler; libraries such as kotlinx-coroutines 1.11 need Kotlin 2.4 metadata.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
        // Compose compiler, for the Liquid Glass tab bar (io.github.kyant0:backdrop is a Compose library).
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
