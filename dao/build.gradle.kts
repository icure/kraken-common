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

dependencies{

    if (rootProject.name != "kraken-common") {
        implementation(project(":kraken-common:logic"))
        implementation(project(":kraken-common:domain"))
    }
    else {
        implementation(project(":logic"))
        implementation(project(":domain"))
    }

    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)

    implementation(coreLibs.springBootCache)
    implementation(coreLibs.springBootWebflux)
    implementation(coreLibs.caffeine)
    implementation(coreLibs.bundles.krouchLibs)
    implementation(coreLibs.taktikBoot)
    
    implementation(coreLibs.apacheCommonsLang3)

    testImplementation(coreLibs.bundles.kotestLibs)
}
