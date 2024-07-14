buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${BuildVersions.agp}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${BuildVersions.kotlin}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
