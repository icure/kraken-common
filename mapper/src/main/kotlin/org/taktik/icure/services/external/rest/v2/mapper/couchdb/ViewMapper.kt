package org.taktik.icure.services.external.rest.v2.mapper.couchdb

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.View
import org.taktik.icure.services.external.rest.v2.dto.couchdb.ViewDto

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface ViewV2Mapper {
	fun map(viewDto: ViewDto): View
	fun map(view: View): ViewDto
}
