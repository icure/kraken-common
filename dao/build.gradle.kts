plugins {
	id("com.icure.kotlin-library-conventions")

	alias(coreLibs.plugins.kotlinAllOpen) apply (true)
	alias(coreLibs.plugins.mavenRepository)
}

group = "org.taktik.icure"

dependencies {

	val projectPrefix =
		when (rootProject.name) {
			"kmehr-importer" -> ":kmehr-module:kraken-common"
			"kraken-common" -> ""
			else -> ":kraken-common"
		}

	implementation(project("$projectPrefix:domain"))
	implementation(project("$projectPrefix:utils"))

	implementation(coreLibs.bundles.kotlinxCoroutinesLibs)

	implementation(coreLibs.springBootCache)
	implementation(coreLibs.springBootWebflux)
	implementation(coreLibs.caffeine)
	implementation(coreLibs.bundles.krouchLibs)
	implementation(coreLibs.taktikBoot)

	implementation(coreLibs.apacheCommonsLang3)
	implementation(coreLibs.bundles.jacksonLibs)
	implementation(coreLibs.commonsCodec)

	testImplementation(coreLibs.bundles.kotestLibs)
}
