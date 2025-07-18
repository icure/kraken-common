package org.taktik.icure.services.external.rest.v1.mapper.couchdb

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.View
import org.taktik.icure.services.external.rest.v1.dto.couchdb.ViewDto

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface ViewMapper {
	fun map(viewDto: ViewDto): View
	fun map(view: View): ViewDto
}
