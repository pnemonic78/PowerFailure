repositories {
    mavenCentral()
}

plugins {
    `kotlin-dsl`
}

sourceSets {
    main {
        java {
            srcDir("../android-lib/buildSrc/src")
        }
    }
}