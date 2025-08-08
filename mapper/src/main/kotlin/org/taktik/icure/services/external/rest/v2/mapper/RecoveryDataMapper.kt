package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.entities.RecoveryData
import org.taktik.icure.services.external.rest.v2.dto.ExchangeDataDto
import org.taktik.icure.services.external.rest.v2.dto.RecoveryDataDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface RecoveryDataV2Mapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(recoveryDataDto: RecoveryDataDto): RecoveryData
	fun map(recoveryData: RecoveryData): RecoveryDataDto
}
