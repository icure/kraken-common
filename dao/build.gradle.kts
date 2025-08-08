@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply (true)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion)
}

val gitVersion: String? by project

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

dependencies {

    val projectPrefix =
        when (rootProject.name) {
            "kmehr-importer" -> ":kmehr-module:kraken-common"
            "kraken-common" -> ""
            else -> ":kraken-common"
        }

    implementation(project("$projectPrefix:domain"))

    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)

    implementation(coreLibs.springBootCache)
    implementation(coreLibs.springBootWebflux)
    implementation(coreLibs.caffeine)
    implementation(coreLibs.bundles.krouchLibs)
    implementation(coreLibs.taktikBoot)

    implementation(coreLibs.apacheCommonsLang3)

    testImplementation(coreLibs.bundles.kotestLibs)
}
