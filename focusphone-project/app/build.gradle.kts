plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.legacy.kapt)
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
        // Keep in sync with the release branch (alfa-X.Y.Z) and bump versionCode on
        // every published build: "about" shows versionName to the user.
        versionCode   = 3
        versionName   = "1.0.6"

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

    testOptions {
        // Robolectric tests that inflate layouts need the app's merged resources (themes,
        // styles); without them a broken style only shows up as a crash on a device.
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        // Release builds run lint-vital and any lint error fails the build. On Windows,
        // if Android Studio holds a lock on the lint cache JARs (FileSystemException),
        // run the build from a terminal with the IDE closed instead of disabling this.
        checkReleaseBuilds = true
        abortOnError       = true
        // No lint baseline on purpose: every error has been fixed at the source, and the
        // remaining warnings are tracked findings in docs/auditoria-tecnica-completa.md.
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