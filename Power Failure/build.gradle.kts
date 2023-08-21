buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${BuildVersions.kotlin_version}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
