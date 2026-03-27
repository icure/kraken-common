plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

group = "org.taktik.icure"

val projectPrefix =
    when (rootProject.name) {
        "kmehr-importer" -> ":kmehr-module:kraken-common"
        "kraken-common" -> ""
        else -> ":kraken-common"
    }

kotlin {
    // Only for kraken and cockpit, not used by cardinal-sdk
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    compilerOptions {
        optIn.add("kotlin.ExperimentalMultiplatform")
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project("$projectPrefix:utils-multiplatform"))
            }
        }
        val commonTest by getting
        val jvmMain by getting {
            dependencies {
                implementation(coreLibs.bundles.jacksonLibs)
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
    }
}
