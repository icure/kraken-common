package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalJwtConfigDto(
	val validationMethod: ValidationMethodDto,
	@Schema(description = "The id of the user in the db.") val id: String? = null,
	@Schema(description = "The name of the field that contains the email in the external JWT.") val emailField: String? = null,
	@Schema(description = "The name of the field that contains the login in the external JWT.") val loginField: String? = null,
	@Schema(description = "The name of the field that contains the phone number in the external JWT.") val mobilePhoneField: String? = null,
	@Schema(description = "The assigner of an identifier plus the name of the field that contains the value of the identifier.") val identifierField: IdentifierSelectorDto? = null,
) {
	@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
	sealed interface ValidationMethodDto {
		data class PublicKey(val key: String): ValidationMethodDto
		data class Oidc(val issureLocation: String): ValidationMethodDto
	}
}
