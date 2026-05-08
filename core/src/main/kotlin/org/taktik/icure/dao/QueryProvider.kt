package org.taktik.icure.dao

import org.springframework.stereotype.Component
import org.taktik.couchdb.Client
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ViewQuery
import org.taktik.icure.asyncdao.impl.GenericDAOImpl
import org.taktik.icure.utils.createQuery

@Component
class QueryProvider(
	private val designDocumentProvider: DesignDocumentProvider,
	private val designDocSchemaCache: DesignDocSchemaCache
) {

	context(dao: GenericDAOImpl<*>)
	suspend fun createQuery(
		client: Client,
		viewName: String,
		secondaryPartition: String? = null
	): ViewQuery {
		return designDocumentProvider.createQuery(
			client = client,
			metadataSource = dao,
			viewName = viewName,
			entityClass = dao.entityClass,
			secondaryPartition = secondaryPartition
		)
	}


}