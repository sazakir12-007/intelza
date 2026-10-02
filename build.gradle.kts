// Every plugin used by the project is declared here so all modules share one build classpath.
plugins {
    alias(libs.plugins.android.application) apply false
    // Not applied anywhere: declaring it pins the Kotlin Gradle plugin version used by
    // AGP's built-in Kotlin support (AGP otherwise brings an older default).
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
