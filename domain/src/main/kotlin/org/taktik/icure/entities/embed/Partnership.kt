/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Partnership(
	val type: PartnershipType? = null, // codes are from CD-CONTACT-PERSON
	val status: PartnershipStatus? = null,
	val partnerId: String? = null, // Person: can either be a patient or a hcp
	@Deprecated("use type instead")
	val meToOtherRelationshipDescription: String? = null, // son if partnerId is my son - codes are from CD-CONTACT-PERSON
	@Deprecated("use type instead")
	val otherToMeRelationshipDescription: String? = null, // father/mother if partnerId is my son
) : Serializable
