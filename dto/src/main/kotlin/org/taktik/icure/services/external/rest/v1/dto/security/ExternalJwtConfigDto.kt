package org.taktik.icure.services.external.rest.v1.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.embed.AuthenticationClassDto

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalJwtConfigDto(
	@get:Schema(description = "Specifies how the external jwt should be validated at login")
	val validationMethod: ValidationMethodDto,
	@get:Schema(description = "Specifies which field of the external jwt should be used to find the matching users")
	val fieldSelector: FieldSelectorDto,
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT)
	@get:Schema(
		description = "The authentication class assigned to the JWT of users that login using this configuration, default is AuthenticationClass.EXTERNAL_AUTHENTICATION",
	)
	val authenticationClass: AuthenticationClassDto = AuthenticationClassDto.EXTERNAL_AUTHENTICATION,
) {
	@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
	sealed interface ValidationMethodDto {
		data class PublicKey(
			val key: String,
		) : ValidationMethodDto

		data class Oidc(
			val issureLocation: String,
		) : ValidationMethodDto
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
	sealed interface FieldSelectorDto {
		data class LocalId(
			val fieldName: String,
		) : FieldSelectorDto

		data class Email(
			val fieldName: String,
		) : FieldSelectorDto

		data class MobilePhone(
			val fieldName: String,
		) : FieldSelectorDto

		data class Username(
			val fieldName: String,
		) : FieldSelectorDto

		data class Identifier(
			val identifierAssigner: String,
			val fieldName: String,
		) : FieldSelectorDto
	}
}
