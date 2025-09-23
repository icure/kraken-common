@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply (true)
    alias(coreLibs.plugins.kotlinSpring) apply (true)
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

    val projectPrefix =
        when (rootProject.name) {
            "kmehr-importer" -> ":kmehr-module:kraken-common"
            "kraken-common" -> ""
            else -> ":kraken-common"
        }

    implementation(project("$projectPrefix:utils"))

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

    implementation(coreLibs.libRecur)
    implementation(coreLibs.apacheCommonsValidator)
    implementation(coreLibs.libPhoneNumber)

    testImplementation(coreLibs.bundles.kotestLibs)
}
