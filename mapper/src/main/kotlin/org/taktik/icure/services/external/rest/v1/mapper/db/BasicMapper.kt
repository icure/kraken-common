package org.taktik.icure.services.external.rest.v1.mapper.db

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.ReplicateCommand.Remote.Authentication.Basic
import org.taktik.icure.services.external.rest.v1.dto.couchdb.BasicDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface BasicMapper {

	fun map(basic: Basic): BasicDto
	fun map(basicDto: BasicDto): Basic
}
