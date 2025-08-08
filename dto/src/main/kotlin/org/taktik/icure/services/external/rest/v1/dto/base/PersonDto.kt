/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.base

import org.taktik.icure.services.external.rest.v1.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v1.dto.embed.GenderDto
import org.taktik.icure.services.external.rest.v1.dto.embed.PersonNameDto
import java.io.Serializable

interface PersonDto :
	Serializable,
	IdentifiableDto<String> {
	val civility: String?
	val gender: GenderDto?
	val firstName: String?
	val lastName: String?
	val companyName: String?
	val names: List<PersonNameDto>
	val addresses: List<AddressDto>
	val languages: List<String>
}
