import com.google.devtools.ksp.gradle.KspTask

plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion)
    alias(coreLibs.plugins.ksp)
    alias(coreLibs.plugins.ktlint)
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

    implementation(project("$projectPrefix:utils"))

    if (rootProject.name == "kraken-cloud") {
        ksp("com.icure:ksp-json-processor")
    }

    implementation(coreLibs.bundles.xmlLibs)
    implementation(coreLibs.bundles.jacksonLibs)
    implementation(coreLibs.bundles.swaggerLibs) {
        exclude(group = "com.fasterxml.jackson.core")
        exclude(group = "org.springframework")
    }

    implementation(coreLibs.bundles.commonsLibs)
    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)
    implementation(coreLibs.bundles.krouchLibs)

    implementation(coreLibs.reflections)
    implementation(coreLibs.guava)
}

tasks.withType<KspTask> {
    onlyIf {
        gradle.startParameter.taskNames.contains(":kraken-common:dto:kspKotlin")
    }
}
