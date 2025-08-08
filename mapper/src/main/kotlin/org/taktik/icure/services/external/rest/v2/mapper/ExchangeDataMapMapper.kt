package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.ExchangeDataMap
import org.taktik.icure.services.external.rest.v2.dto.ExchangeDataMapDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ExchangeDataMapV2Mapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(exchangeDataMapDto: ExchangeDataMapDto): ExchangeDataMap
	fun map(exchangeData: ExchangeDataMap): ExchangeDataMapDto
}
