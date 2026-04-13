plugins {
	id("com.icure.kotlin-library-conventions")

	alias(coreLibs.plugins.kotlinAllOpen) apply (true)
	alias(coreLibs.plugins.kotlinSpring) apply (true)
	alias(coreLibs.plugins.mavenRepository)
	alias(coreLibs.plugins.gitVersion)
	alias(coreLibs.plugins.ksp)
}

val gitVersion: String? by project

group = "org.taktik.icure"
version = gitVersion ?: "0.0.1-SNAPSHOT"

tasks.withType<Test> {
	useJUnitPlatform()
	minHeapSize = "512m"
	maxHeapSize = "16g"
}

val projectPrefix =
	when (rootProject.name) {
		"kmehr-importer" -> ":kmehr-module:kraken-common"
		"kraken-common" -> ""
		else -> ":kraken-common"
	}

dependencies {

	implementation(project("$projectPrefix:utils"))
	api(project("$projectPrefix:customentities-core"))

	if (rootProject.name == "kraken-cloud" || rootProject.name == "kraken-lite") {
		ksp("com.icure:ksp-json-processor")
	}

	implementation(coreLibs.bundles.jacksonLibs)
	implementation(coreLibs.bundles.kotlinxCoroutinesLibs)
	implementation(coreLibs.bundles.springBootLibs)
	implementation(coreLibs.bundles.hibernateValidatorLibs)
	implementation(coreLibs.bundles.commonsLibs)
	implementation(coreLibs.bundles.krouchLibs)

	implementation(coreLibs.krouch)
	implementation(coreLibs.jakartaServlet)
	implementation(coreLibs.taktikCommons)
	implementation(coreLibs.taktikBoot)
	implementation(coreLibs.caffeine)

	implementation(coreLibs.libRecur)

	testImplementation(coreLibs.bundles.kotestLibs)
}

kotlin {
	compilerOptions {
		freeCompilerArgs = listOf("-Xcontext-parameters")
	}
}

if (rootProject.name == "kraken-cloud" || rootProject.name == "kraken-lite") {
	apply(plugin = "generate-mergers-conventions")
}