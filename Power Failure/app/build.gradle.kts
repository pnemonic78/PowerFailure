import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.util.Locale

plugins {
    alias(alibs.plugins.android.application)
    alias(alibs.plugins.kotlin.android)
    alias(alibs.plugins.compose.compiler)
    alias(alibs.plugins.crashlytics)
    alias(alibs.plugins.google.services)
}

val versionMajor = project.properties["APP_VERSION_MAJOR"].toString().toInt()
val versionMinor = project.properties["APP_VERSION_MINOR"].toString().toInt()

android {
    namespace = "net.sf.power.monitor"
    compileSdk = alibs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "net.sf.power.monitor"
        minSdk = alibs.versions.android.minSdk.get().toInt()
        targetSdk = alibs.versions.android.compileSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = (versionMajor * 100) + versionMinor
        versionName = "${versionMajor}.${versionMinor}"

        vectorDrawables.useSupportLibrary = true

        buildConfigField("Boolean", "FEATURE_SMS", "true")
        buildConfigField("Boolean", "CRASHLYTICS", "false")

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
            if (project.hasProperty("RELEASE_STORE_FILE")) {
                val storeFilePath = project.properties["RELEASE_STORE_FILE"] as String
                if (storeFilePath.isNotEmpty()) {
                    storeFile = file(storeFilePath)
                    storePassword = project.properties["RELEASE_STORE_PASSWORD"] as String
                    keyAlias = project.properties["RELEASE_KEY_ALIAS"] as String
                    keyPassword = project.properties["RELEASE_KEY_PASSWORD"] as String
                }
            }
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
        compose = true
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

    flavorDimensions += arrayOf("privacy", "health")

    productFlavors {
        create("google") {
            dimension = "privacy"
            buildConfigField("Boolean", "FEATURE_SMS", "false")
            extraProperties["useGoogleGcm"] = true
        }
        create("regular") {
            dimension = "privacy"
            isDefault = true
        }
        create("crashes") {
            dimension = "health"
            buildConfigField("Boolean", "CRASHLYTICS", "true")
            extraProperties["useGoogleGcm"] = true
        }
        create("silent") {
            dimension = "health"
            isDefault = true
        }
    }
}

dependencies {
    implementation(project(":android-lib:lib"))
    implementation(alibs.material)
    implementation(alibs.log.timber)

    implementation(alibs.bundles.compose)

    testImplementation(alibs.bundles.test)
    androidTestImplementation(alibs.bundles.test.android)
    implementation(alibs.crashlytics)
}

// Disable Google Services plugin for some flavors.
afterEvaluate {
    val useGoogleGcmExtras = mutableMapOf<String, Boolean>()
    android.productFlavors.forEach { flavor ->
        val flavorName = flavor.name
        val extras = flavor.extraProperties
        val useGoogleGcm = extras.has("useGoogleGcm") && (extras["useGoogleGcm"] as Boolean)
        useGoogleGcmExtras[flavorName] = useGoogleGcm
    }
    android.applicationVariants.forEach { variant ->
        val variantName = variant.name.capitalize(Locale.ROOT)

        var useGoogleGcm = false
        variant.productFlavors.forEach { flavor ->
            val flavorName = flavor.name
            useGoogleGcm = useGoogleGcm or (useGoogleGcmExtras[flavorName] ?: false)
        }

        tasks.filter { task ->
            val taskName = task.name
            (taskName.endsWith(variantName) && taskName.contains("Crashlytics"))
                    || taskName.endsWith(variantName + "GoogleServices")
        }.forEach { task ->
            task.enabled = useGoogleGcm
        }
    }
}
