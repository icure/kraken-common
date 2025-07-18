/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import org.taktik.icure.services.external.rest.v1.dto.base.EnumVersionDto
import java.io.Serializable

@EnumVersionDto(1L)
enum class GenderDto(
	val code: String,
) : Serializable {
	male("M"),
	female("F"),
	indeterminate("I"),
	changed("C"),
	changedToMale("Y"),
	changedToFemale("X"),
	unknown("U"),
	;

	companion object {
		fun fromCode(code: String?): GenderDto? = code?.let { c -> entries.find { c == it.code } }
	}
}
