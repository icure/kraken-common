package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.taktik.icure.AlwaysEncrypted
import org.taktik.icure.services.external.rest.v2.dto.base.CryptoActorDto
import java.io.Serializable

@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	property = "type",
)
@JsonSubTypes(
	value = [
		JsonSubTypes.Type(value = DataOwnerWithTypeDto.HcpDataOwner::class, name = "hcp"),
		JsonSubTypes.Type(value = DataOwnerWithTypeDto.PatientDataOwner::class, name = "patient"),
		JsonSubTypes.Type(value = DataOwnerWithTypeDto.DeviceDataOwner::class, name = "device"),
	],
)
sealed interface DataOwnerWithTypeDto : Serializable {
	val dataOwner: CryptoActorDto

	@JsonSerialize
	data class HcpDataOwner(
		override val dataOwner: HealthcarePartyDto,
	) : DataOwnerWithTypeDto

	@JsonSerialize
	data class PatientDataOwner(
		@AlwaysEncrypted override val dataOwner: PatientDto,
	) : DataOwnerWithTypeDto

	@JsonSerialize
	data class DeviceDataOwner(
		override val dataOwner: DeviceDto,
	) : DataOwnerWithTypeDto
}
