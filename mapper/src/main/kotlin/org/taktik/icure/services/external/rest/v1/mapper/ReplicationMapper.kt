/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.couchdb.entity.ReplicatorDocument
import org.taktik.icure.entities.Replication
import org.taktik.icure.services.external.rest.v1.dto.ReplicationDto
import org.taktik.icure.services.external.rest.v1.dto.couchdb.ReplicatorDocumentDto
import org.taktik.icure.services.external.rest.v1.mapper.db.RemoteMapper
import org.taktik.icure.services.external.rest.v1.mapper.db.ReplicationStatsMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DatabaseSynchronizationMapper

@Mapper(componentModel = "spring", uses = [DatabaseSynchronizationMapper::class, RemoteMapper::class, ReplicationStatsMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ReplicationMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(replicationDto: ReplicationDto): Replication
	fun map(replication: Replication): ReplicationDto

	fun map(replicatorDocument: ReplicatorDocument): ReplicatorDocumentDto
}
