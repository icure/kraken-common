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

dependencies {

	val projectPrefix =
		when (rootProject.name) {
			"kmehr-importer" -> ":kmehr-module:kraken-common"
			"kraken-common" -> ""
			else -> ":kraken-common"
		}

	implementation(project("$projectPrefix:utils"))

	if (rootProject.name == "kraken-cloud") {
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

val generateMergersFromJsonTask = tasks.register<org.icure.task.GenerateMergersFromJsonTask>("generateMergersFromJson") {
	inputFolder.set(layout.buildDirectory.dir("generated/ksp/main/resources"))
	outputFolder.set(layout.buildDirectory.dir("generated/ksp/main/kotlin"))

	dependsOn("kspKotlin")
}

// afterEvaluate is fundamental: the kspKotlin task does not exist yet when the script is evaluated, and so the
// finalizedBy cannot be applied otherwise.
afterEvaluate {
	tasks.named("kspKotlin") {
		onlyIf {
			gradle.startParameter.taskNames.contains(":kraken-common:domain:kspKotlin")
		}
		finalizedBy(generateMergersFromJsonTask)
	}
}