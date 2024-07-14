plugins {
    id("com.android.application")
    kotlin("android")
}

val versionMajor = project.properties["APP_VERSION_MAJOR"].toString().toInt()
val versionMinor = project.properties["APP_VERSION_MINOR"].toString().toInt()

android {
    namespace = "net.sf.power.monitor"
    compileSdk = BuildVersions.compileSdk

    defaultConfig {
        applicationId = "net.sf.power.monitor"
        minSdk = BuildVersions.minSdk
        targetSdk = BuildVersions.targetSdk
        versionCode = versionMajor * 100 + versionMinor
        versionName = "${versionMajor}." + versionMinor.toString().padStart(2, '0')

        vectorDrawables.useSupportLibrary = true

        buildConfigField("Boolean", "FEATURE_SMS", "true")

        resourceConfigurations += listOf(
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
            proguardFiles(getDefaultProguardFile("proguard-android.txt"))
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

    kotlinOptions {
        jvmTarget = BuildVersions.jvm.toString()
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
    implementation("com.google.android.material:material:1.12.0")

    // Testing
    testImplementation("junit:junit:${BuildVersions.junit}")
}
