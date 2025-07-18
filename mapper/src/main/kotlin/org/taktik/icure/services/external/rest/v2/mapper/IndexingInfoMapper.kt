package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.domain.IndexingInfo
import org.taktik.icure.services.external.rest.v2.dto.IndexingInfoDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface IndexingInfoV2Mapper {
	fun map(indexingInfoDto: IndexingInfoDto): IndexingInfo
	fun map(indexingInfo: IndexingInfo): IndexingInfoDto
}
