plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

val signingPropsFile = rootProject.file("signing.properties")
val signingProps = Properties().apply {
    if (signingPropsFile.exists()) {
        signingPropsFile.inputStream().use { load(it) }
    }
}
val releaseSigning = signingPropsFile.exists()
        && listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all { signingProps.getProperty(it) != null }

android {
    namespace = "com.aaronroberts.naturesmusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aaronroberts.naturesmusic"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.8.1"
    }

    signingConfigs {
        if (releaseSigning) {
            create("release") {
                storeFile = rootProject.file(signingProps.getProperty("storeFile")!!)
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
