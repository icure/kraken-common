plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply(true)
    alias(coreLibs.plugins.kotlinSpring) apply(true)
}

dependencies {

    val projectPrefix = when(rootProject.name) {
        "kmehr-importer" -> ":kmehr-module:kraken-common"
        "kraken-common" -> ""
        else -> ":kraken-common"
    }

    implementation(project("$projectPrefix:domain"))
    implementation(project("$projectPrefix:logic"))
    implementation(project("$projectPrefix:utils"))

    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)
    implementation(coreLibs.bundles.springBootLibs)
    implementation(coreLibs.bundles.krouchLibs)
}