package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * DTO returned upon successful registration of a data owner, containing the credentials needed for initial login.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.DataOwnerRegistrationSuccessDto")
data class DataOwnerRegistrationSuccessDto(
	/** The login identifier of the newly registered user. */
	@param:Schema(required = true) @ActiveField val userLogin: String,
	/** The unique identifier of the newly created user. */
	@param:Schema(required = true) @ActiveField val userId: String,
	/** The authentication token for the newly registered user. */
	@param:Schema(required = true) @ActiveField val token: String,
)
