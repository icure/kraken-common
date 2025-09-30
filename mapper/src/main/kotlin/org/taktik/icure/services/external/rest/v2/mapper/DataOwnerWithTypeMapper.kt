package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.entities.Patient
import org.taktik.icure.services.external.rest.v2.dto.DataOwnerWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.PatientDto

@Mapper(
	componentModel = "spring",
	uses = [
		PatientV2Mapper::class,
		HealthcarePartyV2Mapper::class,
		DeviceV2Mapper::class,
	],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface DataOwnerWithTypeV2Mapper {
	fun map(
		dataOwnerWithTypeDto: DataOwnerWithTypeDto,
		mapPatientForStore: (PatientDto) -> Patient
	): DataOwnerWithType = when (dataOwnerWithTypeDto) {
		is DataOwnerWithTypeDto.HcpDataOwner -> map(dataOwnerWithTypeDto)
		is DataOwnerWithTypeDto.PatientDataOwner -> map(dataOwnerWithTypeDto, mapPatientForStore)
		is DataOwnerWithTypeDto.DeviceDataOwner -> map(dataOwnerWithTypeDto)
	}
	fun map(
		dataOwnerWithType: DataOwnerWithType,
		mapPatientForRead: (Patient) -> PatientDto
	): DataOwnerWithTypeDto = when (dataOwnerWithType) {
		is DataOwnerWithType.HcpDataOwner -> map(dataOwnerWithType)
		is DataOwnerWithType.PatientDataOwner -> map(dataOwnerWithType, mapPatientForRead)
		is DataOwnerWithType.DeviceDataOwner -> map(dataOwnerWithType)
	}

	fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto.HcpDataOwner): DataOwnerWithType.HcpDataOwner
	fun map(dataOwnerWithType: DataOwnerWithType.HcpDataOwner): DataOwnerWithTypeDto.HcpDataOwner
	@Mappings(
		Mapping(target = "dataOwner", expression = "lambda(mapPatientForStore)"),
	)
	fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto.PatientDataOwner, mapPatientForStore: (PatientDto) -> Patient): DataOwnerWithType.PatientDataOwner
	@Mappings(
		Mapping(target = "dataOwner", expression = "lambda(mapPatientForRead)"),
	)
	fun map(dataOwnerWithType: DataOwnerWithType.PatientDataOwner, mapPatientForRead: (Patient) -> PatientDto): DataOwnerWithTypeDto.PatientDataOwner
	fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto.DeviceDataOwner): DataOwnerWithType.DeviceDataOwner
	fun map(dataOwnerWithType: DataOwnerWithType.DeviceDataOwner): DataOwnerWithTypeDto.DeviceDataOwner
}
