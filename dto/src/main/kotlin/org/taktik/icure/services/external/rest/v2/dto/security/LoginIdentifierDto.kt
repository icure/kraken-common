package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * A structured identifier used to match a user during login, composed of an assigner system and a value within that system.
 */
data class LoginIdentifierDto(
	/** The system or authority that issued or manages this identifier. */
	val assigner: String,
	/** The identifier value within the assigner's namespace. */
	val value: String,
)
