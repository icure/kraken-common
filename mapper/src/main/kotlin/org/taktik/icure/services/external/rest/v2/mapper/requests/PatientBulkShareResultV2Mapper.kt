package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.PatientV2Mapper

// TODO tmp no support yet for generics

interface PatientBulkShareResultV2Mapper {

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToPatient"])
	fun map(bulkShareResultDto: EntityBulkShareResultDto<PatientDto>): EntityBulkShareResult<Patient>

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["patientToDto"])
	fun map(bulkShareResult: EntityBulkShareResult<Patient>): EntityBulkShareResultDto<PatientDto>

	@Named("patientToDto")
	fun patientToDto(patient: Patient?): PatientDto?

	@Named("dtoToPatient")
	fun dtoToPatient(patientDto: PatientDto?): Patient?
}

@Service
class PatientBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val patientMapper: PatientV2Mapper,
) : PatientBulkShareResultV2Mapper {
	override fun map(bulkShareResultDto: EntityBulkShareResultDto<PatientDto>): EntityBulkShareResult<Patient> = EntityBulkShareResult(
		updatedEntity = bulkShareResultDto.updatedEntity?.let { patientMapper.map(it) },
		entityId = bulkShareResultDto.entityId,
		entityRev = bulkShareResultDto.entityRev,
		rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun map(bulkShareResult: EntityBulkShareResult<Patient>): EntityBulkShareResultDto<PatientDto> = EntityBulkShareResultDto(
		updatedEntity =
		bulkShareResult.updatedEntity?.let { patientMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun patientToDto(patient: Patient?): PatientDto? = patient?.let { patientMapper.map(it) }
	override fun dtoToPatient(patientDto: PatientDto?): Patient? = patientDto?.let { patientMapper.map(it) }
}
