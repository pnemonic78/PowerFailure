import org.gradle.api.JavaVersion

object BuildVersions {
    const val agp = "8.4.2"
    const val kotlin = "2.0.0"
    val jvm = JavaVersion.VERSION_11

    const val minSdk = 21
    const val compileSdk = 34
    const val targetSdk = 34

    // App dependencies
    const val androidTest = "1.6.1"
    const val junit = "4.13.2"
    const val junitExt = "1.2.1"
    const val timber = "5.0.1"
}