plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.ktlint)
}

group = "org.taktik.icure"

// Versioned Jackson serialization of the dtos for the Cardinal SDK: the runtime types (CardinalMappers & co.), the
// ObjectMapper factories, and the mix-ins generated from the filtering annotations of the dtos
// (see generate-jackson-filters-conventions in the buildSrc of the including build).
dependencies {

    val projectPrefix =
        when (rootProject.name) {
            "kmehr-importer" -> ":kmehr-module:kraken-common"
            "kraken-common" -> ""
            else -> ":kraken-common"
        }

    implementation(project("$projectPrefix:domain"))
    implementation(project("$projectPrefix:dto"))

    implementation(coreLibs.bundles.jacksonLibs)
    // CardinalMappersProviderImpl is generated as a spring @Configuration
    implementation(coreLibs.springContext)

    testImplementation(coreLibs.bundles.kotestLibs)
}

if (rootProject.name == "kraken-cloud" || rootProject.name == "kraken-lite") {
    apply(plugin = "generate-jackson-filters-conventions")
}
