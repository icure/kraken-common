package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.embed.AuthenticationClassDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalJwtConfigDto(
	val validationMethod: ValidationMethodDto,
	@Schema(description = "The name of the field that contains the icure user local id in the external JWT.") val idField: String? = null,
	@Schema(description = "The name of the field that contains the email in the external JWT.") val emailField: String? = null,
	@Schema(description = "The name of the field that contains the login in the external JWT.") val loginField: String? = null,
	@Schema(description = "The name of the field that contains the phone number in the external JWT.") val mobilePhoneField: String? = null,
	@Schema(description = "The assigner of an identifier plus the name of the field that contains the value of the identifier.") val identifiersFields: Set<IdentifierSelectorDto> = emptySet(),
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	@Schema(description = "The authentication class assigned to the JWT of users that login using this configuration, default is AuthenticationClass.EXTERNAL_AUTHENTICATION")
	val authenticationClass: AuthenticationClassDto = AuthenticationClassDto.EXTERNAL_AUTHENTICATION,
) {
	@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
	sealed interface ValidationMethodDto {
		data class PublicKey(val key: String): ValidationMethodDto
		data class Oidc(val issureLocation: String): ValidationMethodDto
	}
}
