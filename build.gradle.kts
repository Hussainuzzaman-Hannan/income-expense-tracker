// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    // Define properties using 'val' or 'val' instead of 'ext.' in Kotlin DSL
    val kotlin_version by extra("1.9.22") // Updated Kotlin version

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Add this line for MPAndroidChart
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1") // Android Gradle Plugin version
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")

        // NOTE: Do not place your application dependencies here; they belong in the build.gradle.kts file of your app module
    }
}

// The 'allprojects' block for repositories is removed as they should be defined in settings.gradle.kts
// allprojects {
//     repositories {
//         google()
//         mavenCentral()
//         maven { url = uri("https://jitpack.io") }
//     }
// }

// Register tasks using the Kotlin DSL syntax
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
