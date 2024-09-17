/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Article
import org.taktik.icure.pagination.PaginationElement

interface ArticleLogic : EntityPersister<Article>, EntityWithSecureDelegationsLogic<Article> {
	suspend fun createArticle(article: Article): Article?
	suspend fun getArticle(articleId: String): Article?
	suspend fun modifyArticle(article: Article): Article?

	/**
	 * Returns all the [Article]s in a group in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always null key) for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Article]s.
	 */
	fun getAllArticles(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement>
}
