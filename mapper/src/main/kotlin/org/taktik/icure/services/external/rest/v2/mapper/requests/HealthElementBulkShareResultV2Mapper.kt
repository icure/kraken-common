package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.HealthElementDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.HealthElementV2Mapper

// TODO tmp no support yet for generics

interface HealthElementBulkShareResultV2Mapper {
	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["healthElementToDto"])
	fun map(bulkShareResultDto: EntityBulkShareResultDto<HealthElementDto>): EntityBulkShareResult<HealthElement>

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToHealthElement"])
	fun map(bulkShareResult: EntityBulkShareResult<HealthElement>): EntityBulkShareResultDto<HealthElementDto>

	@Named("healthElementToDto")
	fun healthElementToDto(healthElement: HealthElement?): HealthElementDto?

	@Named("dtoToHealthElement")
	fun dtoToHealthElement(healthElementDto: HealthElementDto?): HealthElement?
}

@Service
class HealthElementBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val healthElementMapper: HealthElementV2Mapper,
) : HealthElementBulkShareResultV2Mapper {
	override fun map(bulkShareResultDto: EntityBulkShareResultDto<HealthElementDto>): EntityBulkShareResult<HealthElement> = EntityBulkShareResult(
		updatedEntity = bulkShareResultDto.updatedEntity?.let { healthElementMapper.map(it) },
		entityId = bulkShareResultDto.entityId,
		entityRev = bulkShareResultDto.entityRev,
		rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun map(bulkShareResult: EntityBulkShareResult<HealthElement>): EntityBulkShareResultDto<HealthElementDto> = EntityBulkShareResultDto(
		updatedEntity =
		bulkShareResult.updatedEntity?.let { healthElementMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun healthElementToDto(healthElement: HealthElement?): HealthElementDto? = healthElement?.let { healthElementMapper.map(it) }
	override fun dtoToHealthElement(healthElementDto: HealthElementDto?): HealthElement? = healthElementDto?.let { healthElementMapper.map(it) }
}
