package org.taktik.icure.services.external.rest.v2.mapper.couchdb

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.DesignDocument
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DesignDocumentDto

@Mapper(
	componentModel = "spring",
	uses = [ViewV2Mapper::class],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
interface DesignDocumentV2Mapper {
	fun map(designDocumentDto: DesignDocumentDto): DesignDocument
	fun map(designDocument: DesignDocument): DesignDocumentDto
}
