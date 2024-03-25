package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.ReplicationInfo
import org.taktik.icure.services.external.rest.v2.dto.ReplicationInfoDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ReplicationInfoV2Mapper {
    fun map(replicationInfoDto: ReplicationInfoDto): ReplicationInfo
    fun map(replicationInfo: ReplicationInfo): ReplicationInfoDto
}