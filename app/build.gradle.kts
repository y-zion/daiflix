plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.yzion.daiflix.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yzion.daiflix.tv"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "2.1.0"

        // TV required feature
        manifestPlaceholders["leanbackRequired"] = "true"
        resValue("bool", "daiflix_is_debug", "false")
    }

    // Release signing is opt-in: CI/local builds without credentials still compile,
    // but a release APK is only signed when all four env vars are present.
    val keystoreFile = System.getenv("DAIFLIX_KEYSTORE_FILE")
    val keystorePassword = System.getenv("DAIFLIX_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("DAIFLIX_KEY_ALIAS")
    val keyPassword = System.getenv("DAIFLIX_KEY_PASSWORD")
    val hasReleaseSigning = listOf(keystoreFile, keystorePassword, keyAlias, keyPassword)
        .all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Daiflix Debug")
            resValue("bool", "daiflix_is_debug", "true")
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
    }

    lint {
        // These detector versions are incompatible with AGP 8.7's Kotlin UAST API.
        disable += listOf(
            "NullSafeMutableLiveData",
            "RememberInComposition",
            "FrequentlyChangingValue",
            "AutoboxingStateCreation"
        )
    }
}

ksp {
    // Keep generated Room schemas in version control so migrations are reviewable.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Compose for TV
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Network
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)

    // HTML Parsing
    implementation(libs.jsoup)

    // Image Loading
    implementation(libs.coil.compose)

    // Media Player
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)

    // Leanback

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
