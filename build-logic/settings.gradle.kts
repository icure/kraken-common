/*
 * This file was generated by the Gradle 'init' task.
 *
 * This settings file is used to specify which projects to include in your build-logic build.
 * This project uses @Incubating APIs which are subject to change.
 */

rootProject.name = "kraken-cloud-build-logic"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.taktik.be/content/groups/public") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://repo.spring.io/plugins-release") }
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.taktik.be/content/groups/public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.spring.io/plugins-release") }
    }
}