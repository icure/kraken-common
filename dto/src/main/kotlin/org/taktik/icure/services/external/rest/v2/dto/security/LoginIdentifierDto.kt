package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * A structured identifier used to match a user during login, composed of an assigner system and a value within that system.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.security.LoginIdentifierDto")
data class LoginIdentifierDto(
	/** The system or authority that issued or manages this identifier. */
	@ActiveField val assigner: String,
	/** The identifier value within the assigner's namespace. */
	@ActiveField val value: String,
)
