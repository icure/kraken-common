/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.mapper.couchdb

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.services.external.rest.v2.dto.couchdb.DocIdentifierDto

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
interface DocIdentifierV2Mapper {
	fun map(docIdentifierDto: DocIdentifierDto): DocIdentifier
	fun map(docIdentifier: DocIdentifier): DocIdentifierDto
}
