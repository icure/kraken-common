package org.taktik.icure.services.external.rest.v2.mapper.db

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.ReplicateCommand.Remote.Authentication.Basic
import org.taktik.icure.services.external.rest.v2.dto.couchdb.BasicDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface BasicV2Mapper {

	fun map(basic: Basic): BasicDto
	fun map(basicDto: BasicDto): Basic
}
