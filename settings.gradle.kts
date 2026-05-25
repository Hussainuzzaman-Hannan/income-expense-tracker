// Define plugin management for your project
pluginManagement {
    // These repositories are used to resolve plugins
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// Define how dependencies are resolved for all projects in this build
dependencyResolutionManagement {
    // This mode enforces that all repositories must be declared here,
    // preventing individual project-level build.gradle files from declaring their own.
    // This is the source of the "Build was configured to prefer settings repositories" error
    // if repositories are also declared in allprojects in build.gradle.kts.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // These repositories are used to resolve dependencies for all modules
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // IMPORTANT: Add Jitpack here for MPAndroidChart
    }
}

// Set the root project name
rootProject.name = "IncomeExpenseTracker"

// Include your app module
include(":app")
