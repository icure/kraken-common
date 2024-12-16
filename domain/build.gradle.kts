@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply(true)
    alias(coreLibs.plugins.kotlinSpring) apply(true)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion)
}

val gitVersion: String? by project

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

tasks.withType<Test> {
    useJUnitPlatform()
    minHeapSize = "512m"
    maxHeapSize = "16g"
}

dependencies {
    when (rootProject.name) {
        "kmehr-importer" -> {
            implementation(project(":kmehr-module:kraken-common:utils"))
        }
        "kraken-cloud", "kraken-lite" -> {
            implementation(project(":kraken-common:utils"))
        }
        else -> {
            implementation(project(":utils"))
        }
    }

    implementation(coreLibs.bundles.jacksonLibs)
    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)
    implementation(coreLibs.bundles.springBootLibs)
    implementation(coreLibs.bundles.hibernateValidatorLibs)
    implementation(coreLibs.bundles.commonsLibs)
    implementation(coreLibs.bundles.krouchLibs)

    implementation(coreLibs.krouch)
    implementation(coreLibs.jakartaServlet)
    implementation(coreLibs.taktikCommons)
    implementation(coreLibs.taktikBoot)
    implementation(coreLibs.caffeine)

    testImplementation(coreLibs.bundles.kotestLibs)
}
