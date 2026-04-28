pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("alibs") {
            from(files("android-lib/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Power Failure"
include(":app")
include(":android-lib:kotlin")
include(":android-lib:lib")
