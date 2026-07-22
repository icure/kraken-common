package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.springframework.stereotype.Service
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.CustomEntityBaseDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.CustomEntityBaseV2Mapper

// TODO tmp no support yet for generics

interface CustomEntityBaseBulkShareResultV2Mapper {
	fun map(bulkShareResult: EntityBulkShareResult<CustomEntityBase>, entityId: String): EntityBulkShareResultDto<CustomEntityBaseDto>
	fun mapMinimal(bulkShareResultDto: EntityBulkShareResult<CustomEntityBase>): EntityBulkShareResultDto<Nothing>
}

@Service
class CustomEntityBaseBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val customEntityBaseMapper: CustomEntityBaseV2Mapper,
) : CustomEntityBaseBulkShareResultV2Mapper {
	override fun map(
		bulkShareResult: EntityBulkShareResult<CustomEntityBase>,
		entityId: String,
	): EntityBulkShareResultDto<CustomEntityBaseDto> = EntityBulkShareResultDto(
		updatedEntity = bulkShareResult.updatedEntity?.let { customEntityBaseMapper.map(it, entityId) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun mapMinimal(bulkShareResultDto: EntityBulkShareResult<CustomEntityBase>): EntityBulkShareResultDto<Nothing> =
		EntityBulkShareResultDto(
			updatedEntity = null,
			entityId = bulkShareResultDto.entityId,
			entityRev = bulkShareResultDto.entityRev,
			rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
				k to this.rejectedShareRequestV2Mapper.map(v)
			}.toMap(),
		)
}
