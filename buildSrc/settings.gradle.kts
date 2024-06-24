dependencyResolutionManagement {
	versionCatalogs {
		create("coreLibs") {
			from(files("../libs.versions.toml"))
		}
	}
}
