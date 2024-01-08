package org.taktik.icure.services.external.rest.v2.mapper.utils

import java.io.Serializable
import org.mapstruct.Mapper
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.utils.PaginatedList
import org.taktik.icure.services.external.rest.v2.dto.PaginatedDocumentKeyIdPair

@Mapper(componentModel = "spring")
abstract class PaginatedListV2Mapper {

	fun <U : Identifiable<String>, T : Serializable> map(paginatedList: PaginatedList<U>, mapper: (U) -> T): org.taktik.icure.services.external.rest.v2.dto.PaginatedList<T> {
		return org.taktik.icure.services.external.rest.v2.dto.PaginatedList(
			pageSize = paginatedList.pageSize,
			totalSize = paginatedList.totalSize,
			rows = paginatedList.rows.map { mapper(it) },
			nextKeyPair = paginatedList.nextKeyPair?.let { PaginatedDocumentKeyIdPair(it.startKey, it.startKeyDocId) }
		)
	}
}
