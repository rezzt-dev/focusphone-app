// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.legacy.kapt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// Single entry point for the whole static analysis suite, so CI and humans run the
// same thing: ktlint (formatting, from .editorconfig) plus detekt (code smells).
tasks.register("staticAnalysis") {
    group       = "verification"
    description = "Runs ktlint and detekt over every module."
    dependsOn(subprojects.map { "${it.path}:ktlintCheck" })
    dependsOn(subprojects.map { "${it.path}:detekt" })
}
