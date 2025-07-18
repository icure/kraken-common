/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.db

import java.io.Serializable

/**
 * Created by aduchate on 3/11/13, 14:38
 */
data class PaginationOffset<K>(
	val startKey: K?,
	val startDocumentId: String?,
	val offset: Int?, // should be scarcely used
	val limit: Int,
) : Serializable {
	constructor(limit: Int) : this(null, null, null, limit)

	constructor(limit: Int, startDocumentId: String?) : this(null, startDocumentId, null, limit)

	@Suppress("UNCHECKED_CAST")
	constructor(paginatedList: PaginatedList<*>) : this(
		paginatedList.nextKeyPair?.startKey as K?,
		paginatedList.nextKeyPair?.startKeyDocId,
		null,
		paginatedList.pageSize,
	)

	fun <L> toPaginationOffset(startKeyConverter: (k: K) -> L) = PaginationOffset(this.startKey?.let { startKeyConverter(it) }, this.startDocumentId, this.offset, this.limit)
}
