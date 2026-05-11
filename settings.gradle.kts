/*
 * We're using Stonecutter to make releasing for multiple game versions easier.
 * See https://stonecutter.kikugie.dev/wiki/start/settings
 */

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.3"
}

stonecutter {
    create(rootProject) {
        // Here we specify the target game versions that Stonecutter will try to build the mod for.
        versions("26.1")

        // Specify an explicit target version for Stonecutter to reset to when running the "Reset active version" Gradle task.
        // The task should be run before committing changes, so that there's no diffs from Stonecutter's preprocessor internals.
        vcsVersion = "26.1"
    }
}

rootProject.name = "adventurers-respawns"
