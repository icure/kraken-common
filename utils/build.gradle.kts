@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply (true)
    alias(coreLibs.plugins.mavenRepository)
}

val gitVersion: String? = providers.gradleProperty("gitVersion").orNull

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

dependencies {
    api("com.icure:utils-multiplatform")
    implementation(coreLibs.kotlinxCoroutinesCore)
    implementation(coreLibs.springBootWebflux)
    implementation(coreLibs.kotlinxCoroutinesReactive)
    implementation(coreLibs.kotlinxCoroutinesReactor)
    implementation(coreLibs.apacheCommonsLang3)
    implementation(coreLibs.jacksonKotlin)
}
