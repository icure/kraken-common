/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "A relationship between this patient and another person.")
data class PartnershipDto(
	@get:Schema(description = "Type of relationship.") val type: PartnershipTypeDto? = null, // codes are from CD-CONTACT-PERSON
	@get:Schema(description = "Status of the relationship.") val status: PartnershipStatusDto? = null,
	@get:Schema(description = "UUID of the contact person or patient in this relationship.") val partnerId: String? = null, // PersonDto: can either be a patient or a hcp
	@get:Deprecated("use type instead")
	val meToOtherRelationshipDescription: String? = null, // son if partnerId is my son - codes are from CD-CONTACT-PERSON
	@get:Deprecated("use type instead")
	val otherToMeRelationshipDescription: String? = null, // father/mother if partnerId is my son
) : Serializable
