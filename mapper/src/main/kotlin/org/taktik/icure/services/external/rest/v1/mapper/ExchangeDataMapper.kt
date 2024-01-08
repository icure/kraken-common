package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.ExchangeData
import org.taktik.icure.services.external.rest.v1.dto.ExchangeDataDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ExchangeDataMapper {
    @Mappings(
        Mapping(target = "attachments", ignore = true),
        Mapping(target = "revHistory", ignore = true),
        Mapping(target = "conflicts", ignore = true),
        Mapping(target = "revisionsInfo", ignore = true)
    )
    fun map(exchangeDataDto: ExchangeDataDto): ExchangeData
    fun map(exchangeData: ExchangeData): ExchangeDataDto
}
