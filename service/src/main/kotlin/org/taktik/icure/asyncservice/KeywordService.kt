/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Keyword
import org.taktik.icure.pagination.PaginationElement

interface KeywordService {
	suspend fun createKeyword(keyword: Keyword): Keyword?

	/**
	 * Returns all the [Keyword]s in a group in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Keyword]s.
	 * @throws AccessDeniedException if the current user is not and admin or an healthcare party.
	 */
	fun getAllKeywords(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement>

	/**
	 * Returns all the [Keyword]s in a group.
	 *
	 * @return a [Flow] of [Keyword]s.
	 * @throws AccessDeniedException if the current user is not and admin or an healthcare party.
	 */
	fun getAllKeywords(): Flow<Keyword>
	suspend fun getKeyword(keywordId: String): Keyword?
	fun deleteKeywords(ids: Set<String>): Flow<Keyword>

	suspend fun modifyKeyword(keyword: Keyword): Keyword?
	fun getKeywordsByUser(userId: String): Flow<Keyword>
}
