package org.taktik.icure.services.external.rest.v1.mapper.couchdb

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.couchdb.entity.DesignDocument
import org.taktik.icure.services.external.rest.v1.dto.couchdb.DesignDocumentDto

@Mapper(
	componentModel = "spring",
	uses = [ViewMapper::class],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface DesignDocumentMapper {
	@Mappings(
		Mapping(target = "lib", ignore = true),
	)
	fun map(designDocumentDto: DesignDocumentDto): DesignDocument
	fun map(designDocument: DesignDocument): DesignDocumentDto
}
