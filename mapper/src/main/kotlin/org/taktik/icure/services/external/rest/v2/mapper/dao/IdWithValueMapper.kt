package org.taktik.icure.services.external.rest.v2.mapper.dao

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.dao.IdWithValue
import org.taktik.icure.services.external.rest.v2.dto.dao.IdWithValueDto

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface IdWithValueV2Mapper {
	fun map(idWithValueDto: IdWithValueDto): IdWithValue
	fun map(idWithValue: IdWithValue): IdWithValueDto
}
