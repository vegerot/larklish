import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.ncorti.ktfmt.gradle") version "0.27.0"
}

// Lark credentials live in local.properties (gitignored; written by tools/lark-token) and land
// in BuildConfig. Embedded in the APK for now — see plan.md "Shortcuts".
val local = Properties().apply { rootProject.file("local.properties").inputStream().use(::load) }

android {
    namespace = "com.vegerot.larklish"
    compileSdk = 37 // Compose 1.12 needs 37 to compile; targetSdk stays 36 (runtime behaviour)

    defaultConfig {
        applicationId = "com.vegerot.larklish"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "LARK_APP_ID", "\"${local["lark.appId"]}\"")
        buildConfigField("String", "LARK_APP_SECRET", "\"${local["lark.appSecret"]}\"")
        buildConfigField(
            "String",
            "LARK_USER_REFRESH_TOKEN",
            "\"${local["lark.userRefreshToken"]}\"",
        ) // Layer 5 seed
    }

    testOptions.unitTests.isReturnDefaultValues = true // android.util.Log in JVM tests

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}

// Formatter: `./gradlew ktfmtFormat` rewrites, `./gradlew ktfmtCheck` verifies. The Android Studio
// ktfmt plugin with the same style gives identical output.
ktfmt { kotlinLangStyle() }
