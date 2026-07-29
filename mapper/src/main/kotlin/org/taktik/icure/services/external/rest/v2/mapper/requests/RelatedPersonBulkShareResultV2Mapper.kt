package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.RelatedPersonDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.RelatedPersonV2Mapper

// TODO tmp no support yet for generics

interface RelatedPersonBulkShareResultV2Mapper {
	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["relatedPersonToDto"])
	fun map(bulkShareResultDto: EntityBulkShareResultDto<RelatedPersonDto>): EntityBulkShareResult<RelatedPerson>

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToRelatedPerson"])
	fun map(bulkShareResult: EntityBulkShareResult<RelatedPerson>): EntityBulkShareResultDto<RelatedPersonDto>

	@Named("relatedPersonToDto")
	fun relatedPersonToDto(relatedPerson: RelatedPerson?): RelatedPersonDto?

	@Named("dtoToRelatedPerson")
	fun dtoToRelatedPerson(relatedPersonDto: RelatedPersonDto?): RelatedPerson?
}

@Service
class RelatedPersonBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val relatedPersonMapper: RelatedPersonV2Mapper,
) : RelatedPersonBulkShareResultV2Mapper {
	override fun map(bulkShareResultDto: EntityBulkShareResultDto<RelatedPersonDto>): EntityBulkShareResult<RelatedPerson> = EntityBulkShareResult(
		updatedEntity = bulkShareResultDto.updatedEntity?.let { relatedPersonMapper.map(it) },
		entityId = bulkShareResultDto.entityId,
		entityRev = bulkShareResultDto.entityRev,
		rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun map(bulkShareResult: EntityBulkShareResult<RelatedPerson>): EntityBulkShareResultDto<RelatedPersonDto> = EntityBulkShareResultDto(
		updatedEntity =
		bulkShareResult.updatedEntity?.let { relatedPersonMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun relatedPersonToDto(relatedPerson: RelatedPerson?): RelatedPersonDto? = relatedPerson?.let { relatedPersonMapper.map(it) }
	override fun dtoToRelatedPerson(relatedPersonDto: RelatedPersonDto?): RelatedPerson? = relatedPersonDto?.let { relatedPersonMapper.map(it) }
}
