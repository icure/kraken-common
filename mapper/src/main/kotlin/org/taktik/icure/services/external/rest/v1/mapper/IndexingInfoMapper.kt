package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.domain.IndexingInfo
import org.taktik.icure.services.external.rest.v1.dto.IndexingInfoDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface IndexingInfoMapper {
	fun map(indexingInfoDto: IndexingInfoDto): IndexingInfo
	fun map(indexingInfo: IndexingInfo): IndexingInfoDto
}
