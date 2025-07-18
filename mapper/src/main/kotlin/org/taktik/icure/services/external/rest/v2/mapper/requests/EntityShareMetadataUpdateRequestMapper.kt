package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.requests.EntitySharedMetadataUpdateRequest
import org.taktik.icure.services.external.rest.v2.dto.requests.EntitySharedMetadataUpdateRequestDto

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface EntitySharedMetadataUpdateRequestV2Mapper {
	fun map(requestDto: EntitySharedMetadataUpdateRequestDto): EntitySharedMetadataUpdateRequest
	fun map(request: EntitySharedMetadataUpdateRequest): EntitySharedMetadataUpdateRequestDto
}
