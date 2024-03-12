package org.taktik.icure.services.external.rest.v1.dto.embed

data class RoleConfigurationDto(
	val source: ConfigurationSourceDto,
	val roles: Set<String> = emptySet()
) {

	companion object {
		enum class ConfigurationSourceDto { CONFIGURATION, INHERITED, DEFAULT }
	}

}