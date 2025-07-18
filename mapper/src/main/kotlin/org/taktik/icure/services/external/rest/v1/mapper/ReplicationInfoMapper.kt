package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.ReplicationInfo
import org.taktik.icure.services.external.rest.v1.dto.ReplicationInfoDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ReplicationInfoMapper {
	fun map(replicationInfoDto: ReplicationInfoDto): ReplicationInfo
	fun map(replicationInfo: ReplicationInfo): ReplicationInfoDto
}
