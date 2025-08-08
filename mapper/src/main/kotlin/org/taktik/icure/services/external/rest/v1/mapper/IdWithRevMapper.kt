package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.services.external.rest.v1.dto.IdWithRevDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface IdWithRevMapper {
	fun map(idWithRev: IdWithRevDto): IdAndRev
	fun map(idWithRev: IdAndRev): IdWithRevDto
}
