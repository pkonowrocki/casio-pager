plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.pkonowrocki.casiopager"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.pkonowrocki.casiopager"
        minSdk = 33
        targetSdk = 36
        // Bump both together before tagging v<versionName>. versionCode = major * 10000 + minor * 100 + patch.
        // Literal values on purpose: F-Droid reads them from this file. The release workflow checks them against the tag.
        versionCode = 300
        versionName = "0.3.0"
        // Loki push URL, from the LOKI_URL repository secret; empty means pushing is off until the user sets one
        buildConfigField("String", "LOKI_URL", "\"${System.getenv("LOKI_URL") ?: ""}\"")
    }
    signingConfigs {
        // Release signing needs a real keystore (see android/README.md). Without one, release is signed with the debug key
        // so the APK still installs; such builds cannot update an APK signed with the real key, and the reverse.
        val keystore = System.getenv("ANDROID_KEYSTORE_PATH")
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
    lint {
        abortOnError = true
        lintConfig = file("lint.xml")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
