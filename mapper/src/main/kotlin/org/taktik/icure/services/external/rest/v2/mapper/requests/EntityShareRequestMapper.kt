package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.requests.EntityShareRequest
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityShareRequestDto

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface EntityShareRequestV2Mapper {
	fun map(requestDto: EntityShareRequestDto): EntityShareRequest
	fun map(request: EntityShareRequest): EntityShareRequestDto
}
