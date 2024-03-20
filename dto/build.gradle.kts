import com.icure.codegen.task.PostProcessDtoTask

plugins {
    id("com.icure.kotlin-library-conventions")

    alias(coreLibs.plugins.kotlinAllOpen)
    alias(coreLibs.plugins.mavenRepository)
    alias(coreLibs.plugins.gitVersion)
    alias(coreLibs.plugins.ksp)
}

val gitVersion: String? by project

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

dependencies {
    if (rootProject.name != "kraken-common") {
        implementation(project(":kraken-common:utils"))
    } else {
        implementation(project(":utils"))
    }

    if (rootProject.name == "dto-mapping") {
        ksp(project(":sdk-codegen"))
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

val postProcessDtoTask = tasks.register<PostProcessDtoTask>("PostProcessDtoTask")

tasks.withType<com.google.devtools.ksp.gradle.KspTask> {
    //finalizedBy(postProcessDtoTask)
}