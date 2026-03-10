package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.SdkName
import org.taktik.icure.services.external.rest.v2.dto.embed.AuthenticationClassDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Configuration for authenticating users via an externally-issued JWT. Specifies how the token
 * should be validated and which JWT field is used to locate the matching iCure user.
 */
data class ExternalJwtConfigDto(
	/** The method used to verify the external JWT signature (public-key or OIDC discovery). */
	@param:Schema(description = "Specifies how the external jwt should be validated at login")
	val validationMethod: ValidationMethodDto,
	/** The JWT field selector that identifies which user field to match against. */
	@param:Schema(description = "Specifies which field of the external jwt should be used to find the matching users")
	val fieldSelector: FieldSelectorDto,
	@param:JsonInclude(JsonInclude.Include.NON_DEFAULT)
	@param:Schema(
		description = "The authentication class assigned to the JWT of users that login using this configuration, default is AuthenticationClass.EXTERNAL_AUTHENTICATION",
		defaultValue = "AuthenticationClassDto.EXTERNAL_AUTHENTICATION",
	)
	/** The authentication class assigned to sessions created through this configuration. */
	val authenticationClass: AuthenticationClassDto = AuthenticationClassDto.EXTERNAL_AUTHENTICATION,
) {
	@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
	/**
	 * Sealed interface representing the strategy used to validate the external JWT signature.
	 */
	sealed interface ValidationMethodDto {
		@JsonInclude(JsonInclude.Include.NON_NULL)
		/**
		 * Validates the JWT using a static public key.
		 */
		data class PublicKey(
			/** The PEM-encoded or Base64-encoded public key material. */
			val key: String,
			/** The signature algorithm to use; defaults to the algorithm declared in the JWT header when null. */
			@param:Schema(defaultValue = "null") val signatureAlgorithm: String? = null,
			/** An optional client identifier to verify against the JWT audience claim. */
			val clientId: String? = null,
		) : ValidationMethodDto

		/**
		 * Validates the JWT using OIDC discovery from the specified issuer location.
		 */
		data class Oidc(
			/** The OIDC issuer URL used to retrieve the JWKS for signature verification. */
			@SdkName("issuerLocation")
			@param:JsonAlias("issuerLocation")
			val issureLocation: String,
			/** An optional client identifier to verify against the JWT audience claim. */
			val clientId: String? = null,
		) : ValidationMethodDto
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
	/**
	 * Sealed interface representing the strategy used to extract the user-matching value from the external JWT claims.
	 */
	sealed interface FieldSelectorDto {
		/**
		 * Selects users by matching a JWT field against the user's local identifier.
		 */
		data class LocalId(
			/** The name of the JWT claim containing the local identifier value. */
			val fieldName: String,
		) : FieldSelectorDto

		/**
		 * Selects users by matching a JWT field against the user's email address.
		 */
		data class Email(
			/** The name of the JWT claim containing the email value. */
			val fieldName: String,
		) : FieldSelectorDto

		/**
		 * Selects users by matching a JWT field against the user's mobile phone number.
		 */
		data class MobilePhone(
			/** The name of the JWT claim containing the mobile phone value. */
			val fieldName: String,
		) : FieldSelectorDto

		/**
		 * Selects users by matching a JWT field against the user's username.
		 */
		data class Username(
			/** The name of the JWT claim containing the username value. */
			val fieldName: String,
		) : FieldSelectorDto

		/**
		 * Selects users by matching a JWT field against a structured identifier with a specific assigner.
		 */
		data class Identifier(
			/** The assigner system for the identifier to match against. */
			val identifierAssigner: String,
			/** The name of the JWT claim containing the identifier value. */
			val fieldName: String,
		) : FieldSelectorDto
	}
}
