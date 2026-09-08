plugins {
    alias(libs.plugins.android.application)
    id("com.android.legacy-kapt") version "9.2.1"
}

android {
    namespace  = "app.focusphone"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "app.focusphone"
        minSdk        = 24
        targetSdk     = 36
        versionCode   = 2
        versionName   = "1.0.4"

        testInstrumentationRunner = "app.focusphone.TestRunner"
    }

    signingConfigs {
        create("release") {
            val releaseStoreFile = findProperty("RELEASE_STORE_FILE") as String?
            if (releaseStoreFile != null) {
                storeFile = file(releaseStoreFile)
                storePassword = findProperty("RELEASE_STORE_PASSWORD") as String
                keyAlias = findProperty("RELEASE_KEY_ALIAS") as String
                keyPassword = findProperty("RELEASE_KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            val releaseStoreFile = findProperty("RELEASE_STORE_FILE") as String?
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        // Disable the lint-vital task on release builds.
        // The lintVitalAnalyzeRelease task causes a FileSystemException on Windows
        // when Android Studio holds a lock on the lint cache JARs during the build.
        // Run ./gradlew :app:lint separately for a full lint report.
        checkReleaseBuilds = false
    }
}

dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)

    // Lifecycle (ViewModel + LiveData)
    implementation(libs.androidx.lifecycle.vm)
    implementation(libs.androidx.lifecycle.ld)
    implementation(libs.androidx.lifecycle.runtime)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Material 3
    implementation(libs.material)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    "kapt"(libs.room.compiler)

    // Testing — JVM unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)

    // Testing — Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
}