package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.services.external.rest.v2.dto.DataOwnerWithTypeDto

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
	suspend fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto): DataOwnerWithType = when (dataOwnerWithTypeDto) {
		is DataOwnerWithTypeDto.HcpDataOwner -> map(dataOwnerWithTypeDto)
		is DataOwnerWithTypeDto.PatientDataOwner -> map(dataOwnerWithTypeDto)
		is DataOwnerWithTypeDto.DeviceDataOwner -> map(dataOwnerWithTypeDto)
	}
	suspend fun map(dataOwnerWithType: DataOwnerWithType): DataOwnerWithTypeDto = when (dataOwnerWithType) {
		is DataOwnerWithType.HcpDataOwner -> map(dataOwnerWithType)
		is DataOwnerWithType.PatientDataOwner -> map(dataOwnerWithType)
		is DataOwnerWithType.DeviceDataOwner -> map(dataOwnerWithType)
	}

	suspend fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto.HcpDataOwner): DataOwnerWithType.HcpDataOwner
	suspend fun map(dataOwnerWithType: DataOwnerWithType.HcpDataOwner): DataOwnerWithTypeDto.HcpDataOwner
	suspend fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto.PatientDataOwner): DataOwnerWithType.PatientDataOwner
	suspend fun map(dataOwnerWithType: DataOwnerWithType.PatientDataOwner): DataOwnerWithTypeDto.PatientDataOwner
	suspend fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto.DeviceDataOwner): DataOwnerWithType.DeviceDataOwner
	suspend fun map(dataOwnerWithType: DataOwnerWithType.DeviceDataOwner): DataOwnerWithTypeDto.DeviceDataOwner
}
