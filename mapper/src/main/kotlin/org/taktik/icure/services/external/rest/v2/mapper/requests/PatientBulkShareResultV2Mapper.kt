package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.springframework.stereotype.Service
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.PatientV2Mapper

// TODO tmp no support yet for generics

interface PatientBulkShareResultV2Mapper {
	fun map(bulkShareResult: EntityBulkShareResult<Patient>): EntityBulkShareResultDto<PatientDto>
	fun mapMinimal(bulkShareResultDto: EntityBulkShareResult<Patient>): EntityBulkShareResultDto<Nothing>
}

@Service
class PatientBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val patientMapper: PatientV2Mapper,
) : PatientBulkShareResultV2Mapper {
	override fun map(bulkShareResult: EntityBulkShareResult<Patient>): EntityBulkShareResultDto<PatientDto> = EntityBulkShareResultDto(
		updatedEntity = bulkShareResult.updatedEntity?.let { patientMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun mapMinimal(bulkShareResultDto: EntityBulkShareResult<Patient>): EntityBulkShareResultDto<Nothing> =
		EntityBulkShareResultDto(
			updatedEntity = null,
			entityId = bulkShareResultDto.entityId,
			entityRev = bulkShareResultDto.entityRev,
			rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
				k to this.rejectedShareRequestV2Mapper.map(v)
			}.toMap(),
		)
}
