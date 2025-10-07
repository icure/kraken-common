package org.taktik.icure.services.external.rest.v2.mapper.requests

import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.PatientV2Mapper

// TODO tmp no support yet for generics

interface PatientBulkShareResultV2Mapper {
	fun map(bulkShareResultDto: EntityBulkShareResultDto<PatientDto>, mapPatient: (PatientDto) -> Patient): EntityBulkShareResult<Patient>

	fun map(bulkShareResult: EntityBulkShareResult<Patient>, mapPatient: (Patient) -> PatientDto): EntityBulkShareResultDto<PatientDto>
}

@Service
class PatientBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
) : PatientBulkShareResultV2Mapper {
	override fun map(bulkShareResultDto: EntityBulkShareResultDto<PatientDto>, mapPatient: (PatientDto) -> Patient): EntityBulkShareResult<Patient> = EntityBulkShareResult(
		updatedEntity = bulkShareResultDto.updatedEntity?.let { mapPatient(it) },
		entityId = bulkShareResultDto.entityId,
		entityRev = bulkShareResultDto.entityRev,
		rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun map(bulkShareResult: EntityBulkShareResult<Patient>, mapPatient: (Patient) -> PatientDto): EntityBulkShareResultDto<PatientDto> = EntityBulkShareResultDto(
		updatedEntity = bulkShareResult.updatedEntity?.let { mapPatient(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)
}
