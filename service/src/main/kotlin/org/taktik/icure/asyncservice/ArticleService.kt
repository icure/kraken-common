/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Article
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface ArticleService : EntityWithSecureDelegationsService<Article> {
	suspend fun createArticle(article: Article): Article?

	/**
	 * Deletes [Article]s in batch.
	 * If the user does not meet the precondition to delete [Article]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [Set] containing the ids of the [Article]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [Article]s that were successfully deleted.
	 */
	fun deleteArticles(ids: List<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [Article].
	 *
	 * @param articleId the id of the [Article] to delete.
	 * @return a [DocIdentifier] related to the [Article] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [Article].
	 * @throws [NotFoundRequestException] if an [Article] with the specified [articleId] does not exist.
	 */
	suspend fun deleteArticle(articleId: String): DocIdentifier
	suspend fun getArticle(articleId: String): Article?

	/**
	 * Returns all the [Article]s in a group in a format for pagination.
	 * Note: this method will filter out all the entities that the current user is now allowed to access, but it will
	 * guarantee that the limit specified in the [paginationOffset] is reached as long as there are available elements.
	 *
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always null key) for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Article]s.
	 */
	fun getAllArticles(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement>
	suspend fun modifyArticle(article: Article): Article?
}
