package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.DataOwnerWithType
import org.taktik.icure.services.external.rest.v1.dto.DataOwnerWithTypeDto

@Mapper(
	componentModel = "spring",
	uses = [
		PatientMapper::class,
		HealthcarePartyMapper::class,
		DeviceMapper::class,
	],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface DataOwnerWithTypeMapper {
	fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto): DataOwnerWithType = when (dataOwnerWithTypeDto) {
		is DataOwnerWithTypeDto.HcpDataOwner -> map(dataOwnerWithTypeDto)
		is DataOwnerWithTypeDto.PatientDataOwner -> map(dataOwnerWithTypeDto)
		is DataOwnerWithTypeDto.DeviceDataOwner -> map(dataOwnerWithTypeDto)
	}
	fun map(dataOwnerWithType: DataOwnerWithType): DataOwnerWithTypeDto = when (dataOwnerWithType) {
		is DataOwnerWithType.HcpDataOwner -> map(dataOwnerWithType)
		is DataOwnerWithType.PatientDataOwner -> map(dataOwnerWithType)
		is DataOwnerWithType.DeviceDataOwner -> map(dataOwnerWithType)
	}

	fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto.HcpDataOwner): DataOwnerWithType.HcpDataOwner
	fun map(dataOwnerWithType: DataOwnerWithType.HcpDataOwner): DataOwnerWithTypeDto.HcpDataOwner
	fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto.PatientDataOwner): DataOwnerWithType.PatientDataOwner
	fun map(dataOwnerWithType: DataOwnerWithType.PatientDataOwner): DataOwnerWithTypeDto.PatientDataOwner
	fun map(dataOwnerWithTypeDto: DataOwnerWithTypeDto.DeviceDataOwner): DataOwnerWithType.DeviceDataOwner
	fun map(dataOwnerWithType: DataOwnerWithType.DeviceDataOwner): DataOwnerWithTypeDto.DeviceDataOwner
}
