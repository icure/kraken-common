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
        implementation(project(":kraken-common:dto"))
        implementation(project(":kraken-common:utils"))
    }
    else {
        implementation(project(":domain"))
        implementation(project(":dto"))
        implementation(project(":utils"))
    }

    implementation(coreLibs.bundles.jsonWebTokenLibs) {
        exclude(group = "com.fasterxml.jackson.core")
    }

    implementation(coreLibs.springBootSecurity)
}
