plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("io.kotest") version coreLibs.versions.kotest
    id("com.google.devtools.ksp") version coreLibs.versions.ksp
}

group = "org.taktik.icure"

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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
        }
        val commonTest by getting {
            dependencies {
                implementation(coreLibs.kotestEngine)
                implementation(coreLibs.kotestAssertionsCore)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(coreLibs.bundles.jacksonLibs)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(coreLibs.kotestRunnerJunit5)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(coreLibs.kotlinDateTime)
            }
        }
        val jsTest by getting
    }
}

dependencies {
    add("kspJvm", "com.icure:ksp-json-processor")
}