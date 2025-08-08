package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface RejectedShareRequestV2Mapper {
	fun map(rejectedShareOrMetadataUpdateRequestDto: EntityBulkShareResultDto.RejectedShareOrMetadataUpdateRequestDto): EntityBulkShareResult.RejectedShareOrMetadataUpdateRequest
	fun map(rejectedShareOrMetadataUpdateRequest: EntityBulkShareResult.RejectedShareOrMetadataUpdateRequest): EntityBulkShareResultDto.RejectedShareOrMetadataUpdateRequestDto
}
