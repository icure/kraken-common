package org.taktik.icure.services.external.rest.v2.mapper.db

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.ReplicateCommand.Remote
import org.taktik.icure.services.external.rest.v2.dto.couchdb.RemoteDto

@Mapper(componentModel = "spring", uses = [RemoteAuthenticationV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface RemoteV2Mapper {

	fun map(remote: Remote): RemoteDto
	fun map(remoteDto: RemoteDto): Remote
}
