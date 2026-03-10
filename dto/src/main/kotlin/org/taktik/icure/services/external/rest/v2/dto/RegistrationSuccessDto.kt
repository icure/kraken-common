package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * DTO returned upon successful user and group registration, containing the identifiers and
 * initial authentication token needed to access the newly created environment.
 */
data class RegistrationSuccessDto(
	/** The identifier of the newly created group. */
	@param:Schema(required = true) val groupId: String,
	/** The identifier of the newly created user. */
	@param:Schema(required = true) val userId: String,
	/** The initial authentication token for the newly registered user. */
	@param:Schema(required = true) val token: String,
)
