package org.taktik.icure.services.external.rest.v1.dto.embed

data class RoleConfigurationDto(
	val source: SourceDto,
	val roles: Set<String> = emptySet()
) {
	enum class SourceDto { CONFIGURATION, INHERITED, DEFAULT }
}