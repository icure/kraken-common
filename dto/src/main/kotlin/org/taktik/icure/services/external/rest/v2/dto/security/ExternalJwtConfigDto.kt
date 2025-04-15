package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalJwtConfigDto(
	@Schema(description = "The public key to verify the JWTs of the external service, as a base64 string.") val publicKey: Base64StringDto,
	@Schema(description = "The configuration to identify the users based on the JWT claims.") val externalJwtSelectorDto: ExternalJwtSelectorDto
)