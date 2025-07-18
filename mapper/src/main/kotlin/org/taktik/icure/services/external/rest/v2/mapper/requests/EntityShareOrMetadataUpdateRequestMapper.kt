package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.springframework.stereotype.Service
import org.taktik.icure.entities.requests.BulkShareOrUpdateMetadataParams
import org.taktik.icure.entities.requests.EntityShareOrMetadataUpdateRequest
import org.taktik.icure.entities.requests.ShareEntityRequestDetails
import org.taktik.icure.services.external.rest.v2.dto.requests.BulkShareOrUpdateMetadataParamsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityShareOrMetadataUpdateRequestDto

interface EntityShareOrMetadataUpdateRequestV2Mapper {
	fun map(requestDto: EntityShareOrMetadataUpdateRequestDto): EntityShareOrMetadataUpdateRequest

	fun map(
		bulkRequests: BulkShareOrUpdateMetadataParamsDto,
	): BulkShareOrUpdateMetadataParams
}

@Service
class EntityShareOrMetadataUpdateRequestV2MapperImpl(
	private val entityShareRequestV2Mapper: EntityShareRequestV2Mapper,
	private val entitySharedMetadataUpdateRequestV2Mapper: EntitySharedMetadataUpdateRequestV2Mapper,
) : EntityShareOrMetadataUpdateRequestV2Mapper {
	override fun map(
		requestDto: EntityShareOrMetadataUpdateRequestDto,
	): EntityShareOrMetadataUpdateRequest = requestDto.share?.let { shareRequest ->
		if (requestDto.update != null) {
			throw IllegalArgumentException("A request should be either 'share' or 'update'")
		}
		entityShareRequestV2Mapper.map(shareRequest)
	} ?: requestDto.update?.let { updateRequest ->
		entitySharedMetadataUpdateRequestV2Mapper.map(updateRequest)
	} ?: throw IllegalArgumentException("A request should be either 'share' or 'update'")

	override fun map(
		bulkRequests: BulkShareOrUpdateMetadataParamsDto,
	) = BulkShareOrUpdateMetadataParams(
		bulkRequests.requestsByEntityId.mapValues { (_, value) ->
			ShareEntityRequestDetails(
				value.requests.mapValues { (_, value) ->
					map(value)
				},
				value.potentialParentDelegations,
			)
		},
	)
}
