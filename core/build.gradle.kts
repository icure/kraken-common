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

    val projectPrefix = when (rootProject.name) {
        "kmehr-importer" -> ":kmehr-module:kraken-common"
        "kraken-common" -> ""
        else -> ":kraken-common"
    }

    implementation(project("$projectPrefix:logic"))
    implementation(project("$projectPrefix:mapper"))
    implementation(project("$projectPrefix:dto"))
    implementation(project("$projectPrefix:domain"))
    implementation(project("$projectPrefix:dao"))
    implementation(project("$projectPrefix:jwt"))
    implementation(project("$projectPrefix:utils"))
    implementation(project("$projectPrefix:service"))

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
    implementation(coreLibs.bundles.bouncyCastleLibs)
    implementation(coreLibs.jakartaServlet)

    implementation(coreLibs.kotlinxCollectionsImmutableJvm)
    implementation(coreLibs.springSession)
    implementation(coreLibs.gcpAuthProvider)
    implementation(coreLibs.taktikBoot)
    implementation(coreLibs.caffeine)
    implementation(coreLibs.kotp)
    implementation(coreLibs.kotlinDateTime)
    implementation(coreLibs.libRecur)
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.72.Final:osx-aarch_64")
    implementation(coreLibs.googleApiClient)
    implementation(coreLibs.websocketCommons)

    testImplementation(coreLibs.jupiter)
    testImplementation(coreLibs.mockk)
    testImplementation(coreLibs.springBootTest)
    testImplementation(coreLibs.springmockk)
    testImplementation(coreLibs.betterParse)
    testImplementation(coreLibs.reflections)
    testImplementation(coreLibs.kotlinxSerializationJson)
    testImplementation(coreLibs.kotlinxCoroutinesTest)

    testImplementation(coreLibs.bundles.kotestLibs)
    testImplementation(coreLibs.bundles.hibernateValidatorLibs)
}

tasks.withType<com.google.devtools.ksp.gradle.KspTask> {
    onlyIf {
        gradle.startParameter.taskNames.contains(":kraken-common:core:kspKotlin")
    }
}
