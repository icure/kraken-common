/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.EntityTemplateDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.EntityCacheFactory
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.EntityTemplate
import org.taktik.icure.utils.distinctById

@Repository("entityTemplateDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.EntityTemplate' && !doc.deleted) emit( null, doc._id )}")
class EntityTemplateDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<EntityTemplate>(EntityTemplate::class.java, couchDbDispatcher, idGenerator, entityCacheFactory.getConfiguredCache(), designDocumentProvider, daoConfig = daoConfig), EntityTemplateDAO {

	@View(name = "by_user_type_descr", map = "classpath:js/entitytemplate/By_user_type_descr.js")
	override fun listEntityTemplatesByUserIdTypeDescr(datastoreInformation: IDatastoreInformation, userId: String, type: String, searchString: String?, includeEntities: Boolean?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val descr = if (searchString != null) sanitizeString(searchString) else null
		val viewQuery = createQuery(datastoreInformation, "by_user_type_descr").startKey(ComplexKey.of(userId, type, descr)).endKey(
			ComplexKey.of(
				userId, type,
				(
					descr
						?: ""
					) + "\ufff0"
			)
		).includeDocs(includeEntities ?: false)

		emitAll(
			(
				if (viewQuery.isIncludeDocs)
					client.queryViewIncludeDocs<ComplexKey, EntityTemplate, EntityTemplate>(viewQuery).mapNotNull { it.doc }
				else
					client.queryView<ComplexKey, EntityTemplate>(viewQuery).mapNotNull { it.value }
				).distinctById()
		)
	}

	@View(name = "by_type_descr", map = "classpath:js/entitytemplate/By_type_descr.js")
	override fun listEntityTemplatesByTypeDescr(datastoreInformation: IDatastoreInformation, type: String, searchString: String?, includeEntities: Boolean?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val descr = if (searchString != null) sanitizeString(searchString) else null
		val viewQuery = createQuery(datastoreInformation, "by_type_descr").startKey(ComplexKey.of(type, descr)).endKey(
			ComplexKey.of(
				type,
				(
					descr
						?: ""
					) + "\ufff0"
			)
		).includeDocs(includeEntities ?: false)

		emitAll(
			(
				if (viewQuery.isIncludeDocs)
					client.queryViewIncludeDocs<ComplexKey, EntityTemplate, EntityTemplate>(viewQuery).mapNotNull { it.doc }
				else
					client.queryView<ComplexKey, EntityTemplate>(viewQuery).mapNotNull { it.value }
				).distinctById()
		)
	}

	@View(name = "by_user_type_keyword", map = "classpath:js/entitytemplate/By_user_type_keyword.js")
	override fun listEntityTemplatesByUserIdTypeKeyword(
		datastoreInformation: IDatastoreInformation,
		userId: String?,
		type: String?,
		keyword: String?,
		includeEntities: Boolean?
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "by_user_type_descr").startKey(ComplexKey.of(userId, type, keyword)).endKey(
			ComplexKey.of(
				userId, type,
				(
					keyword
						?: ""
					) + "\ufff0"
			)
		).includeDocs(includeEntities ?: false)

		emitAll(
			(
				if (viewQuery.isIncludeDocs)
					client.queryViewIncludeDocs<ComplexKey, EntityTemplate, EntityTemplate>(viewQuery).mapNotNull { it.doc }
				else
					client.queryView<ComplexKey, EntityTemplate>(viewQuery).mapNotNull { it.value }
				).distinctById()
		)
	}

	@View(name = "by_type_keyword", map = "classpath:js/entitytemplate/By_type_keyword.js")
	override fun listEntityTemplatesByTypeAndKeyword(datastoreInformation: IDatastoreInformation, type: String?, keyword: String?, includeEntities: Boolean?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery = createQuery(datastoreInformation, "by_type_keyword").startKey(ComplexKey.of(type, keyword)).endKey(
			ComplexKey.of(
				type,
				(
					keyword
						?: ""
					) + "\ufff0"
			)
		).includeDocs(includeEntities ?: false)

		emitAll(
			(
				if (viewQuery.isIncludeDocs)
					client.queryViewIncludeDocs<ComplexKey, EntityTemplate, EntityTemplate>(viewQuery).mapNotNull { it.doc }
				else
					client.queryView<ComplexKey, EntityTemplate>(viewQuery).mapNotNull { it.value }
				).distinctById()
		)
	}
}
