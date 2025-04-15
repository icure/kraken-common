package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalJwtConfigDto(
	@Schema(description = "The public key of the external service.") val publicKey: Base64StringDto,
	@Schema(description = "The id of the user in the db.") val id: String? = null,
	@Schema(description = "The name of the field that contains the email in the external JWT.") val emailField: String? = null,
	@Schema(description = "The name of the field that contains the login in the external JWT.") val loginField: String? = null,
	@Schema(description = "The name of the field that contains the phone number in the external JWT.") val mobilePhoneField: String? = null,
	@Schema(description = "The assigner of an identifier plus the name of the field that contains the value of the identifier.") val identifierField: IdentifierSelectorDto? = null,
)