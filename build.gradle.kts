// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.10.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0-RC" apply false
    id("com.google.devtools.ksp") version "2.1.20-2.0.0" apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
    id("com.android.library") version "8.10.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}
