package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.SdkName

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * DTO containing the information required to register a new user and group in the iCure platform.
 */
data class RegistrationInformationDto(
	/** The identifier of the application or project for this registration. */
	@param:JsonAlias("projectId")
	@SdkName("projectId")
	val applicationId: String? = null,
	/** The first name of the person registering. */
	val firstName: String? = null,
	/** The last name of the person registering. */
	val lastName: String? = null,
	/** The name of the company or organization. */
	val companyName: String? = null,
	/** The email address of the person registering. */
	@param:Schema(required = true) val emailAddress: String,
	/** Additional user options serialized as a string. */
	val userOptions: String? = null,
	/** The set of roles to assign to the newly created user. */
	val userRoles: Set<String> = emptySet(),
	/** The minimum required Kraken version for this registration. */
	val minimumKrakenVersion: String? = null,
	/** The target cluster for the registration. */
	val cluster: String? = null,
)
