/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncdao.KeywordDAO
import org.taktik.icure.asynclogic.KeywordLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Keyword
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class KeywordLogicImpl(
	private val keywordDAO: KeywordDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters
) : GenericLogicImpl<Keyword, KeywordDAO>(fixer, datastoreInstanceProvider, filters), KeywordLogic{
	private val log = LoggerFactory.getLogger(KeywordLogicImpl::class.java)

	override fun getGenericDAO(): KeywordDAO {
		return keywordDAO
	}

	override suspend fun createKeyword(keyword: Keyword) =
		fix(keyword) { fixedKeyword ->
			if(fixedKeyword.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
			val createdKeywords = try { // Setting Keyword attributes
				createEntities(setOf(fixedKeyword))
			} catch (e: Exception) {
				log.error("createKeyword: " + e.message)
				throw IllegalArgumentException("Invalid Keyword", e)
			}
			createdKeywords.firstOrNull()
		}

	override suspend fun getKeyword(keywordId: String): Keyword? {
		val datastoreInformation = getInstanceAndGroup()
		return keywordDAO.getKeyword(datastoreInformation, keywordId)
	}

	override fun deleteKeywords(ids: Set<String>): Flow<DocIdentifier> =
		flow {
			try {
				emitAll(deleteEntities(ids))
			} catch (e: Exception) {
				log.error(e.message, e)
			}
		}

	override suspend fun modifyKeyword(keyword: Keyword): Keyword? =
		modifyEntities(setOf(keyword)).firstOrNull()

	override fun getKeywordsByUser(userId: String): Flow<Keyword> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(keywordDAO.getKeywordsByUserId(datastoreInformation, userId))
		}

	override fun getAllKeywords(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(keywordDAO
			.getAllPaginated(datastoreInformation, paginationOffset.limitIncludingKey(), Nothing::class.java)
			.toPaginatedFlow<Keyword>(paginationOffset.limit)
		)
	}

}
