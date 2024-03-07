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

    if (rootProject.name != "kraken-common") {
        implementation(project(":kraken-common:domain"))
        implementation(project(":kraken-common:jwt"))
        implementation(project(":kraken-common:utils"))
    }
    else {
        implementation(project(":domain"))
        implementation(project(":jwt"))
        implementation(project(":utils"))
    }

    implementation(coreLibs.bundles.jacksonLibs)
    implementation(coreLibs.bundles.springBootLibs)
    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)
    implementation(coreLibs.krouch)
    implementation(coreLibs.kotlinxCollectionsImmutableJvm)
    implementation(coreLibs.jakartaServlet)
    implementation(coreLibs.guava)
    implementation(coreLibs.bouncyCastleBcprov)
}
