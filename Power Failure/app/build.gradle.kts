import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val versionMajor = project.properties["APP_VERSION_MAJOR"].toString().toInt()
val versionMinor = project.properties["APP_VERSION_MINOR"].toString().toInt()

android {
    namespace = "net.sf.power.monitor"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "net.sf.power.monitor"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = (versionMajor * 100) + versionMinor
        versionName = "${versionMajor}.${versionMinor}"

        vectorDrawables.useSupportLibrary = true

        buildConfigField("Boolean", "FEATURE_SMS", "true")

        androidResources {
            localeFilters += setOf(
                "af",
                "ar",
                "bg",
                "cs",
                "da",
                "de",
                "el",
                "es",
                "et",
                "fa",
                "fi",
                "fr",
                "hi",
                "hu",
                "in",
                "it",
                "iw",
                "ja",
                "ko",
                "lt",
                "lv",
                "ms",
                "nb",
                "nl",
                "pl",
                "pt",
                "ro",
                "ru",
                "sv",
                "th",
                "tr",
                "uk",
                "vi",
                "zh",
                "zu"
            )
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = project.properties["STORE_PASSWORD_RELEASE"] as String
            keyAlias = "release"
            keyPassword = project.properties["KEY_PASSWORD_RELEASE"] as String
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            // disabled until fix proguard issues: minifyEnabled true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = BuildVersions.jvm
        targetCompatibility = BuildVersions.jvm
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(BuildVersions.jvm.toString())
        }
    }

    flavorDimensions += "privacy"

    productFlavors {
        create("google") {
            dimension = "privacy"
            buildConfigField("Boolean", "FEATURE_SMS", "false")
        }
        create("regular") {
            dimension = "privacy"
            isDefault = true
        }
    }
}

dependencies {
    implementation(project(":android-lib:lib"))
    implementation(libs.material)
    implementation(libs.log.timber)

    testImplementation(libs.bundles.test)
    androidTestImplementation(libs.bundles.test.android)
}