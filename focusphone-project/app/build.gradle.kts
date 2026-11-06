import io.gitlab.arturbosch.detekt.Detekt
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
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
        // versionName is "<x.y.z>-beta" while in beta and matches the changelog section
        // "[x.y.z]" and the git tag "v<x.y.z>"; "about" shows it to the user. Bump
        // versionCode on every published build (1.0.0-beta and 1.4-beta both shipped as 3).
        versionCode   = 4
        versionName   = "1.4.0-beta"

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

// ── Static analysis ───────────────────────────────────────────────────────────
// Android Lint covers Android/API correctness; these two cover Kotlin itself.
// ktlint is the formatter (rules come from `.editorconfig` at the repository root)
// and detekt is the code-smell analyser (rules in `config/detekt/detekt.yml`).
// Run both with `./gradlew staticAnalysis`, autofix formatting with `./gradlew ktlintFormat`.

ktlint {
    // Pinned in the version catalog so the ktlint engine never moves on its own: a
    // silent engine bump would rewrite files that CI previously accepted.
    version.set(libs.versions.ktlint)
    // Fail the build on violations. No baseline on purpose, same policy as Android
    // Lint above: violations are fixed at the source, not parked in a file.
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
    filter {
        // Generated sources (view binding, Room, BuildConfig) are never hand-edited.
        exclude { it.file.path.contains("${layout.buildDirectory.get()}") }
    }
}

detekt {
    parallel               = true
    // Start from detekt's defaults and only record the deviations in the project
    // config, so upgrading detekt brings its new rules in instead of silently
    // keeping a frozen copy of an old ruleset.
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom(
        "src/main/java",
        "src/test/java",
        "src/androidTest/java",
    )
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = JavaVersion.VERSION_11.toString()
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
        txt.required.set(false)
        md.required.set(false)
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