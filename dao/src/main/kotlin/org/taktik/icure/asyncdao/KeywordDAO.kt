/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Keyword

interface KeywordDAO : GenericDAO<Keyword> {

	/**
	 * Returns all the [Keyword]s in a group in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Keyword]s.
	 */
	fun getAllKeywords(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<Nothing>): Flow<ViewQueryResultEvent>
	suspend fun getKeyword(datastoreInformation: IDatastoreInformation, keywordId: String): Keyword?

	fun getKeywordsByUserId(datastoreInformation: IDatastoreInformation, userId: String): Flow<Keyword>
}
