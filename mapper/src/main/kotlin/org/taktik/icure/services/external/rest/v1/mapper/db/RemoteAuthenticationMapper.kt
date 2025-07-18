package org.taktik.icure.services.external.rest.v1.mapper.db

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.ReplicateCommand.Remote.Authentication
import org.taktik.icure.services.external.rest.v1.dto.couchdb.RemoteAuthenticationDto

@Mapper(componentModel = "spring", uses = [BasicMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface RemoteAuthenticationMapper {

	fun map(remoteAuthentication: Authentication): RemoteAuthenticationDto
	fun map(remoteAuthenticationDto: RemoteAuthenticationDto): Authentication
}
