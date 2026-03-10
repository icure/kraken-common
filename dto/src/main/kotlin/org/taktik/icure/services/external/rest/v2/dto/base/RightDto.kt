package org.taktik.icure.services.external.rest.v2.dto.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Data transfer object representing a set of access rights defined by user names and roles.
 */
data class RightDto(
	/** The set of user names that have this right. */
	val names: Set<String> = emptySet(),
	/** The set of roles that have this right. */
	val roles: Set<String> = emptySet(),
)
