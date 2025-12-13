// Ambil properti dari local.properties (SINTAKS KOTLIN)
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.catchlearn.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.catchlearn.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY", "")
        if (geminiApiKey.isEmpty()) {
            logger.warn("GEMINI_API_KEY is not set in local.properties. API calls will fail.")
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            @Suppress("UnstableApiUsage")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // Diperlukan untuk library Gemini
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true // WAJIB ada
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Library CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // =======================================
    // PERBAIKAN: Menambahkan library Gemini, Guava, dan Desugar
    // =======================================
    implementation(libs.generativeai)
    implementation(libs.guava.android)

    // =======================================
    // PERBAIKAN: MENGGUNAKAN TITIK (.) BUKAN UNDERSCORE (_)
    // =======================================
    coreLibraryDesugaring(libs.desugar.jdk.libs) // <-- Ini perbaikannya


    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

}

