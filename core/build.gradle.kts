import com.github.jk1.license.render.CsvReportRenderer
import com.github.jk1.license.render.ReportRenderer

plugins {
    id("com.icure.kotlin-library-conventions")
    kotlin("plugin.serialization")

    alias(coreLibs.plugins.springBootPlugin) apply(true)
    alias(coreLibs.plugins.springBootDependenciesManagement) apply(true)
    alias(coreLibs.plugins.kotlinAllOpen) apply(true)
    alias(coreLibs.plugins.kotlinSpring) apply(true)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion) apply(true)
    alias(coreLibs.plugins.helmRepository) apply(true)
    alias(coreLibs.plugins.licenceReport) apply(true)
    alias(coreLibs.plugins.ksp) apply(true)
    `maven-publish`
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(CsvReportRenderer())
}

dependencies {

    when (rootProject.name) {
        "kmehr-importer" -> {
            implementation(project(":kmehr-module:kraken-common:core"))
            implementation(project(":kmehr-module:kraken-common:dao"))
            implementation(project(":kmehr-module:kraken-common:domain"))
            implementation(project(":kmehr-module:kraken-common:logic"))
            implementation(project(":kmehr-module:kraken-common:mapper"))
            implementation(project(":kmehr-module:kraken-common:dto"))
            implementation(project(":kmehr-module:kraken-common:utils"))
        }
        "kraken-cloud" -> {
            implementation(project(":kraken-common:core"))
            implementation(project(":kraken-common:dao"))
            implementation(project(":kraken-common:domain"))
            implementation(project(":kraken-common:logic"))
            implementation(project(":kraken-common:mapper"))
            implementation(project(":kraken-common:dto"))
            implementation(project(":kraken-common:utils"))
        }
        else -> {
            implementation(project(":core"))
            implementation(project(":dao"))
            implementation(project(":domain"))
            implementation(project(":logic"))
            implementation(project(":mapper"))
            implementation(project(":dto"))
            implementation(project(":utils"))
        }
    }

    if (rootProject.name == "kraken-cloud") {
        ksp("com.icure:ksp-json-processor")
    }

    implementation(coreLibs.hibernateValidator)
    implementation(coreLibs.bundles.springBootLibs)
    implementation(coreLibs.bundles.jacksonLibs)
    implementation(coreLibs.bundles.springLibs)
    implementation(coreLibs.bundles.springSecurityLibs)
    implementation(coreLibs.bundles.krouchLibs)
    implementation(coreLibs.bundles.commonsLibs)
    implementation(coreLibs.bundles.kotlinxCoroutinesLibs)
    implementation(coreLibs.bundles.swaggerLibs)

    implementation(coreLibs.bundles.jsonWebTokenLibs) {
        exclude(group = "com.fasterxml.jackson.core")
    }
    implementation(coreLibs.bundles.bouncyCastleLibs)
    implementation(coreLibs.jakartaServlet)

    implementation(coreLibs.kotlinxCollectionsImmutableJvm)
    implementation(coreLibs.springSession)
    implementation(coreLibs.mapperProcessor)
    implementation(coreLibs.gcpAuthProvider)
    implementation(coreLibs.taktikBoot)
    implementation(coreLibs.caffeine)
    implementation(coreLibs.kotp)
    implementation(coreLibs.kotlinDateTime)
    implementation(coreLibs.mapstruct)
    implementation(coreLibs.libRecur)
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.72.Final:osx-aarch_64")
    implementation(coreLibs.googleApiClient)
    implementation(coreLibs.websocketCommons)

    testImplementation(coreLibs.jupiter)
    testImplementation(coreLibs.mockk)
    testImplementation(coreLibs.springBootTest)
    testImplementation(coreLibs.springmockk)
    testImplementation(coreLibs.betterParse)
    testImplementation(coreLibs.icureTestSetup)
    testImplementation(coreLibs.reflections)
    testImplementation(coreLibs.kotlinxSerialization)
    testImplementation(coreLibs.kotlinxCoroutinesTest)

    testImplementation(coreLibs.bundles.kotestLibs)
    testImplementation(coreLibs.bundles.hibernateValidatorLibs)
}

tasks.withType<com.google.devtools.ksp.gradle.KspTask> {
    onlyIf {
        gradle.startParameter.taskNames.contains(":kraken-common:core:kspKotlin")
    }
}
