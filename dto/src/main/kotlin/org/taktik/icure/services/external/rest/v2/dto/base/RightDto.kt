package org.taktik.icure.services.external.rest.v2.dto.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object representing a set of access rights defined by user names and roles.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.base.RightDto")
data class RightDto(
	/** The set of user names that have this right. */
	@ActiveField val names: Set<String> = emptySet(),
	/** The set of roles that have this right. */
	@ActiveField val roles: Set<String> = emptySet(),
)
