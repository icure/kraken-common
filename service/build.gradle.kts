plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply(true)
    alias(coreLibs.plugins.kotlinSpring) apply(true)
}

dependencies {

    when (rootProject.name) {
        "kmehr-importer" -> {
            implementation(project(":kmehr-module:kraken-common:domain"))
            implementation(project(":kmehr-module:kraken-common:logic"))
            implementation(project(":kmehr-module:kraken-common:utils"))
        }
        "kraken-cloud", "kraken-lite" -> {
            implementation(project(":kraken-common:domain"))
            implementation(project(":kraken-common:logic"))
            implementation(project(":kraken-common:utils"))
        }
        else -> {
            implementation(project(":domain"))
            implementation(project(":logic"))
            implementation(project(":utils"))
        }
    }

    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)
    implementation(coreLibs.bundles.springBootLibs)
    implementation(coreLibs.bundles.krouchLibs)
}