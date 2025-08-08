package org.taktik.icure.services.external.rest.v1.mapper.db

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.ReplicationStats
import org.taktik.icure.services.external.rest.v1.dto.couchdb.ReplicationStatsDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ReplicationStatsMapper {

	fun map(replicationStats: ReplicationStats): ReplicationStatsDto
	fun map(replicationStatsDto: ReplicationStatsDto): ReplicationStats
}
