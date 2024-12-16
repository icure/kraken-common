@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed (https://youtrack.jetbrains.com/issue/KTIJ-19369)
plugins {
	id("com.icure.kotlin-library-conventions")

	alias(coreLibs.plugins.ksp)
	alias(coreLibs.plugins.mavenRepository)
	alias(coreLibs.plugins.gitVersion)
	alias(coreLibs.plugins.kotlinAllOpen)
}

val gitVersion: String? by project

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

dependencies {
	ksp(group = "io.icure", name = "kmap", version = coreLibs.versions.kmap.orNull)

	when (rootProject.name) {
		"kmehr-importer" -> {
			implementation(project(":kmehr-module:kraken-common:domain"))
			implementation(project(":kmehr-module:kraken-common:dto"))
			implementation(project(":kmehr-module:kraken-common:utils"))
		}
		"kraken-common" -> {
			implementation(project(":domain"))
			implementation(project(":dto"))
			implementation(project(":utils"))
		}
		else -> {
			implementation(project(":kraken-common:domain"))
			implementation(project(":kraken-common:dto"))
			implementation(project(":kraken-common:utils"))
		}
	}

	implementation(coreLibs.bundles.jacksonLibs)
	implementation(coreLibs.bundles.springBootLibs)
	implementation(coreLibs.bundles.swaggerLibs)
	implementation(coreLibs.bundles.krouchLibs)

	implementation(coreLibs.krouch)
	implementation(coreLibs.mapstruct)
	implementation(coreLibs.kmapKsp) {
		exclude(group = "ch.qos.logback")
	}

	testImplementation(coreLibs.bundles.kotestLibs)
}

tasks.register("KspPreCheck") {
	inputs.dir(project(":kraken-common:domain").file("src/main/kotlin/org/taktik/icure/domain"))
	inputs.dir(project(":kraken-common:domain").file("src/main/kotlin/org/taktik/icure/entities"))

	inputs.dir(project(":kraken-common:dto").file("src/main/kotlin/org/taktik/icure/dto"))
	inputs.dir(project(":kraken-common:dto").file("src/main/kotlin/org/taktik/icure/services/external/rest/v1/dto"))
	inputs.dir(project(":kraken-common:dto").file("src/main/kotlin/org/taktik/icure/services/external/rest/v2/dto"))

	inputs.dir("src/main/kotlin/org/taktik/icure/services/external/rest/v1/mapper")
	inputs.dir("src/main/kotlin/org/taktik/icure/services/external/rest/v2/mapper")
	inputs.dir("src/main/kotlin/org/taktik/icure/services/external/rest")

	outputs.dir("build/generated/ksp/main/kotlin")
	doLast {
		println("Checking for modifications in mappers")
	}
}

tasks.withType<com.google.devtools.ksp.gradle.KspTask> {
	dependsOn("KspPreCheck")
}

// KSP doesn't like incremental compiling
// If the KspPreCheck task is-up-to date, then it means that the annotated files were not modified
// So the KSP can be disabled
gradle.taskGraph.whenReady {
	gradle.taskGraph.beforeTask {
		if (this.name == "kspKotlin") {
			this.enabled = tasks.asMap["KspPreCheck"]?.state?.upToDate?.not() ?: true
		}
	}
}
