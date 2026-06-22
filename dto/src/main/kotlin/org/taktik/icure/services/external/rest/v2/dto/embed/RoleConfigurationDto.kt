package org.taktik.icure.services.external.rest.v2.dto.embed

import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Represents the role configuration for a user, specifying the source of the roles and the set of assigned roles.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.RoleConfigurationDto")
data class RoleConfigurationDto(
	/** The source from which the roles are derived (configuration, inherited, or default). */
	@param:Schema(required = true)
	@ActiveField val source: SourceDto,
	/** The set of role identifiers assigned. */
	@ActiveField val roles: Set<String> = emptySet(),
) {
	enum class SourceDto { CONFIGURATION, INHERITED, DEFAULT }
}
