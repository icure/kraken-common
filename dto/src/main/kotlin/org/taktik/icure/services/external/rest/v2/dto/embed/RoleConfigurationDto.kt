package org.taktik.icure.services.external.rest.v2.dto.embed

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Represents the role configuration for a user, specifying the source of the roles and the set of assigned roles.
 */
data class RoleConfigurationDto(
	/** The source from which the roles are derived (configuration, inherited, or default). */
	@param:Schema(required = true)
	val source: SourceDto,
	/** The set of role identifiers assigned. */
	val roles: Set<String> = emptySet(),
) {
	enum class SourceDto { CONFIGURATION, INHERITED, DEFAULT }
}
