package org.taktik.icure.services.external.rest.v2.mapper.db

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.ReplicateCommand.Remote.Authentication
import org.taktik.icure.services.external.rest.v2.dto.couchdb.RemoteAuthenticationDto

@Mapper(componentModel = "spring", uses = [BasicV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface RemoteAuthenticationV2Mapper {

	fun map(remoteAuthentication: Authentication): RemoteAuthenticationDto
	fun map(remoteAuthenticationDto: RemoteAuthenticationDto): Authentication
}
