/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.embed

@Suppress("EnumEntryName")
enum class DeactivationReasonDto {
	deceased,
	moved,
	other_doctor,
	retired,
	no_contact,
	unknown,
	none,
}
