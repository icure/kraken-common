@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply (true)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion)
}

val gitVersion: String? by project

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

dependencies {
    implementation(coreLibs.kotlinxCoroutinesCore)
    implementation(coreLibs.springBootWebflux)
    implementation(coreLibs.kotlinxCoroutinesReactive)
    implementation(coreLibs.kotlinxCoroutinesReactor)
    implementation(coreLibs.apacheCommonsLang3)
    implementation(coreLibs.jacksonKotlin)
}
