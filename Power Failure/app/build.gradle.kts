plugins {
    id("com.android.application")
    kotlin("android")
}

val versionMajor = (project.properties["APP_VERSION_MAJOR"] as String).toInt()
val versionMinor = (project.properties["APP_VERSION_MINOR"] as String).toInt()

android {
    compileSdk = BuildVersions.compileSdkVersion

    defaultConfig {
        applicationId = "net.sf.power.monitor"
        minSdk = BuildVersions.minSdkVersion
        targetSdk = BuildVersions.targetSdkVersion
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

    sourceSets {
        getByName("androidTest") {
            java { srcDir(file("src/androidTest/kotlin")) }
        }
        getByName("main") {
            java { srcDir(file("src/main/kotlin")) }
        }
        getByName("test") {
            java { srcDir(file("src/test/kotlin")) }
        }
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
    implementation("com.google.android.material:material:1.9.0")

    // Testing
    testImplementation("junit:junit:${BuildVersions.junitVersion}")
}