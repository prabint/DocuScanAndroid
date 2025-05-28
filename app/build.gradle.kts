import java.io.FileInputStream
import java.util.Properties

plugins {
    kotlin("kapt")
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "prabin.timsina.documentscanner"
    compileSdk = 36
    defaultConfig {
        applicationId = "prabin.timsina.documentscanner"
        minSdk = 29
        targetSdk = 36
        // Allows creating apk with dynamic version in CI (e.g. ./gradlew build -PversionCode=3)
        versionCode = project.findProperty("versionCode")?.toString()?.toInt() ?: 1
        versionName = project.findProperty("versionName")?.toString() ?: "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["useTestStorageService"] = "true"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    // Signs release build via android studio itself. Release build should have better performance.
    signingConfigs {
        signingConfigs {
            create("release") {
                val keystorePropertiesFile = rootProject.file("keystore.properties")
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))

                storeFile = file(
                    System.getenv("RELEASE_STORE_FILE")
                        ?: file(keystoreProperties["RELEASE_STORE_FILE"] as String),
                )
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: keystoreProperties["RELEASE_KEY_ALIAS"] as String
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                    ?: keystoreProperties["RELEASE_STORE_PASSWORD"] as String
                keyPassword =
                    System.getenv("RELEASE_KEY_PASSWORD") ?: keystoreProperties["RELEASE_KEY_PASSWORD"] as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":opencv"))

    // For CustomObjectDetectorOptions
    implementation("com.google.mlkit:object-detection-custom:17.0.2")

    // DI
    implementation("com.google.dagger:hilt-android:2.56.2")
    kapt("com.google.dagger:hilt-android-compiler:2.56.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Camera
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("androidx.camera:camera-extensions:1.4.2")

    /*
     * This library makes navigation very easy.
     *
     * Github: https://github.com/raamcosta/compose-destinations
     * Wiki: https://composedestinations.rafaelcosta.xyz/setup/
     */
    implementation("io.github.raamcosta.compose-destinations:core:1.10.0")
    implementation("io.github.raamcosta.compose-destinations:animations-core:1.10.0")
    ksp("io.github.raamcosta.compose-destinations:ksp:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Font
    implementation("androidx.compose.ui:ui-text-google-fonts:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.50")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.56.2")

    /**
     * For saving screenshots when using .writeToTestStorage(). Screenshots saved in:
     * build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/YOUR_DEVICE_NAME/foo.png
     *
     * Does not work if using adb to run test, only works with gradle.
     *
     * Without this we get "TestStorageException: No content provider registered for:
     * content://androidx.test.services.storage.outputfiles/foo.png. Are all test services apks installed?".
     */
    androidTestUtil("androidx.test.services:test-services:1.5.0")

    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")

    // Resolves "No static method forceEnableAppTracing()V in class Landroidx/tracing/Trace"
    implementation("androidx.tracing:tracing:1.3.0")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}
