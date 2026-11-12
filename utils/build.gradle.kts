plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen) apply (true)
    alias(coreLibs.plugins.mavenRepository)
}

group = "org.taktik.icure"

dependencies {
    api("com.icure:utils-multiplatform")
    implementation(coreLibs.kotlinxCoroutinesCore)
    implementation(coreLibs.springBootWebflux)
    implementation(coreLibs.kotlinxCoroutinesReactive)
    implementation(coreLibs.kotlinxCoroutinesReactor)
    implementation(coreLibs.apacheCommonsLang3)
    implementation(coreLibs.jacksonKotlin)
}
