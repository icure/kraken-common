pluginManagement {
    includeBuild("./build-logic")

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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Required otherwise plugin coreLibs.plugins.mavenRepository breaks the configuration of repositories
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.taktik.be/content/groups/public") }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.spring.io/plugins-release") }
    }

    versionCatalogs {
        create("coreLibs") {
            from(files("./libs.versions.toml"))
        }
    }
}

rootProject.name = "kraken-common"

include("domain", "logic", "core", "mapper", "dto", "dao", "jwt", "service", "utils")
