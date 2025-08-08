/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import org.taktik.icure.services.external.rest.v1.dto.base.EnumVersionDto
import java.io.Serializable

@Suppress("EnumEntryName")
@EnumVersionDto(1L)
enum class PersonalStatusDto : Serializable {
	single,
	in_couple,
	married,
	separated,
	divorced,
	divorcing,
	widowed,
	widower,
	complicated,
	unknown,
	contract,
	other,
	annulled,
	polygamous,
}
