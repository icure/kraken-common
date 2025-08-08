/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import org.taktik.icure.services.external.rest.v1.dto.base.EnumVersionDto

@Suppress("EnumEntryName")
@EnumVersionDto(1L)
enum class VisibilityDto {
	maskedfromsummary,
	maskedfromexportedfile,
	proeminent,
	highlighted,
	visible,
}
