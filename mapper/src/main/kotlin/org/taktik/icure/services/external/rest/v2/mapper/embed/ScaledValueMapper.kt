package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.taktik.icure.entities.embed.ScaledValue
import org.taktik.icure.services.external.rest.v2.dto.embed.ScaledValueDto

interface ScaledValueV2Mapper {
	fun map(scaledValue: ScaledValue): ScaledValueDto
	fun map(scaledValueDto: ScaledValueDto): ScaledValue
}