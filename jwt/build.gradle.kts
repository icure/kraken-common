plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply (true)
    alias(coreLibs.plugins.mavenRepository)
}

group = "org.taktik.icure"

dependencies {

    val projectPrefix =
        when (rootProject.name) {
            "kmehr-importer" -> ":kmehr-module:kraken-common"
            "kraken-common" -> ""
            else -> ":kraken-common"
        }

    implementation(project("$projectPrefix:domain"))
    implementation(project("$projectPrefix:dto"))
    implementation(project("$projectPrefix:utils"))

    implementation(coreLibs.springBootSecurity)
    implementation(coreLibs.springBootOauth2)
    implementation(coreLibs.caffeine)
    implementation(coreLibs.kotlinxCoroutinesReactor)
}
