@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply(true)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion)
}

val gitVersion: String? by project

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

dependencies {

    val projectPrefix = when (rootProject.name) {
        "kmehr-importer" -> ":kmehr-module:kraken-common"
        "kraken-common" -> ""
        else -> ":kraken-common"
    }

    implementation(project("$projectPrefix:domain"))
    implementation(project("$projectPrefix:jwt"))
    implementation(project("$projectPrefix:utils"))

    implementation(coreLibs.bundles.jacksonLibs)
    implementation(coreLibs.bundles.springBootLibs)
    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)
    implementation(coreLibs.krouch)
    implementation(coreLibs.kotlinxCollectionsImmutableJvm)
    implementation(coreLibs.jakartaServlet)
    implementation(coreLibs.guava)
    implementation(coreLibs.bouncyCastleBcprov)
}
