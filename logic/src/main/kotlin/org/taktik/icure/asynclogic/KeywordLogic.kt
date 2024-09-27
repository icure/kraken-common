/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Keyword
import org.taktik.icure.pagination.PaginationElement

interface KeywordLogic : EntityPersister<Keyword> {
	suspend fun createKeyword(keyword: Keyword): Keyword?

	suspend fun getKeyword(keywordId: String): Keyword?

	suspend fun modifyKeyword(keyword: Keyword): Keyword?
	fun getKeywordsByUser(userId: String): Flow<Keyword>

	/**
	 * Returns all the [Keyword]s in a group in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Keyword]s.
	 */
	fun getAllKeywords(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement>
}
