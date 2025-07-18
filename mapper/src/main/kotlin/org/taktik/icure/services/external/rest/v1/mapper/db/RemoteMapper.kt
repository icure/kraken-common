package org.taktik.icure.services.external.rest.v1.mapper.db

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.ReplicateCommand.Remote
import org.taktik.icure.services.external.rest.v1.dto.couchdb.RemoteDto

@Mapper(componentModel = "spring", uses = [RemoteAuthenticationMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface RemoteMapper {

	fun map(remote: Remote): RemoteDto
	fun map(remoteDto: RemoteDto): Remote
}
