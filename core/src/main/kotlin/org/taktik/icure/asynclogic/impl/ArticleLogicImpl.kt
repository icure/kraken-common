/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncdao.ArticleDAO
import org.taktik.icure.asynclogic.ArticleLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.base.impl.EncryptableEntityLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.Article
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.exceptions.DeletionException
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class ArticleLogicImpl(
    private val articleDAO: ArticleDAO,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    sessionLogic: SessionInformationProvider,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    fixer: Fixer
) : EncryptableEntityLogic<Article, ArticleDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic), ArticleLogic {

	override suspend fun createArticle(article: Article) = fix(article) { fixedArticle ->
		if(fixedArticle.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		val datastoreInformation = getInstanceAndGroup()
		articleDAO.create(datastoreInformation, fixedArticle)
	}


	override fun deleteArticles(ids: List<String>): Flow<DocIdentifier> =flow {
		try {
			emitAll(deleteEntities(ids))
		} catch (e: Exception) {
			throw DeletionException(e.message, e)
		}
	}

	override fun deleteArticles(ids: Flow<String>): Flow<DocIdentifier> =flow {
		try {
			emitAll(deleteEntities(ids))
		} catch (e: Exception) {
			throw DeletionException(e.message, e)
		}
	}

	override suspend fun getArticle(articleId: String): Article? = getEntity(articleId)

	override suspend fun modifyArticle(article: Article) =
		fix(article) { fixedArticle ->
			val datastoreInformation = getInstanceAndGroup()
			articleDAO.save(datastoreInformation, fixedArticle)
		}

	override fun getArticles() = getEntities()
	override fun entityWithUpdatedSecurityMetadata(entity: Article, updatedMetadata: SecurityMetadata): Article {
		return entity.copy(securityMetadata = updatedMetadata)
	}

	override fun getGenericDAO() = articleDAO
}
