plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.kotest") version coreLibs.versions.kotest
    id("com.google.devtools.ksp") version coreLibs.versions.ksp
}

group = "org.taktik.icure"

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
        }
        val commonTest by getting
        val jvmMain by getting {
            dependencies {
                implementation(coreLibs.bundles.jacksonLibs)
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(coreLibs.kotlinDateTime)
            }
        }
        val jsTest by getting
    }
}
