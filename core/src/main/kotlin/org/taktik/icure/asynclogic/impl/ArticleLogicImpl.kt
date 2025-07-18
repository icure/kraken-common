/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ArticleDAO
import org.taktik.icure.asynclogic.ArticleLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Article
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class ArticleLogicImpl(
	private val articleDAO: ArticleDAO,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : EntityWithEncryptionMetadataLogic<Article, ArticleDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters),
	ArticleLogic {
	override suspend fun createArticle(article: Article) = fix(article, isCreate = true) { fixedArticle ->
		if (fixedArticle.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		val datastoreInformation = getInstanceAndGroup()
		articleDAO.create(datastoreInformation, fixedArticle)
	}

	override suspend fun getArticle(articleId: String): Article? = getEntity(articleId)

	override suspend fun modifyArticle(article: Article) = fix(article, isCreate = false) { fixedArticle ->
		val datastoreInformation = getInstanceAndGroup()
		articleDAO.save(datastoreInformation, fixedArticle)
	}

	override fun getAllArticles(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			articleDAO
				.getAllPaginated(datastoreInformation, paginationOffset.limitIncludingKey(), Nothing::class.java)
				.toPaginatedFlow<Article>(paginationOffset.limit),
		)
	}

	override fun entityWithUpdatedSecurityMetadata(
		entity: Article,
		updatedMetadata: SecurityMetadata,
	): Article = entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO() = articleDAO
}
