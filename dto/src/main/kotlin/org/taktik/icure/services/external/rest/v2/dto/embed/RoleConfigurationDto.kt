package org.taktik.icure.services.external.rest.v2.dto.embed

import io.swagger.v3.oas.annotations.media.Schema

data class RoleConfigurationDto(
	@get:Schema(required = true)
	val source: SourceDto,
	val roles: Set<String> = emptySet(),
) {
	enum class SourceDto { CONFIGURATION, INHERITED, DEFAULT }
}
