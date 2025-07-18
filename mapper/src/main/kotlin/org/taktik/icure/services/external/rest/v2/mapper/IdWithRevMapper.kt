package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.services.external.rest.v2.dto.IdWithRevDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface IdWithRevV2Mapper {
	fun map(idWithRev: IdWithRevDto): IdAndRev
	fun map(idWithRev: IdAndRev): IdWithRevDto
}
