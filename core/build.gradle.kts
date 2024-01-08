plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.springBootPlugin) apply(true)
    alias(coreLibs.plugins.springBootDependenciesManagement) apply(true)
    alias(coreLibs.plugins.kotlinAllOpen) apply(true)
    alias(coreLibs.plugins.kotlinSpring) apply(true)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion) apply(true)
    alias(coreLibs.plugins.helmRepository) apply(true)
    alias(coreLibs.plugins.kotlinxSerialization) apply(true)
    alias(coreLibs.plugins.licenceReport) apply(true)

    `maven-publish`
}

dependencies {
    implementation(project(":kraken-common:logic"))
    implementation(project(":kraken-common:mapper"))
    implementation(project(":kraken-common:dto"))
    implementation(project(":kraken-common:domain"))
    implementation(project(":kraken-common:dao"))
    implementation(project(":kraken-common:jwt"))
    implementation(project(":kraken-common:utils"))
    implementation(project(":kraken-common:service"))

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
    implementation(coreLibs.bundles.javaxElLibs)
    implementation(coreLibs.bundles.bouncyCastleLibs)

    implementation(coreLibs.kotlinxCollectionsImmutableJvm)
    implementation(coreLibs.springSession)
    implementation(coreLibs.mapperProcessor)
    implementation(coreLibs.gcpAuthProvider)
    implementation(coreLibs.taktikBoot)
    implementation(coreLibs.caffeine)
    implementation(coreLibs.javaxServlet)
    implementation(coreLibs.jboss)
    implementation(coreLibs.mapstruct)
    implementation(coreLibs.libRecur)
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.72.Final:osx-aarch_64")
    implementation(coreLibs.googleApiClient)

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
    testImplementation(coreLibs.bundles.ktorServerLibs)
    testImplementation(coreLibs.bundles.ktorClientLibs)
}
