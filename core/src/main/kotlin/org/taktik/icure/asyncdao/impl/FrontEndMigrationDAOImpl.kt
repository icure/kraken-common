/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryViewIncludeDocs
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.FrontEndMigrationDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.entities.FrontEndMigration

@Repository("frontEndMigrationDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.FrontEndMigration' && !doc.deleted) emit( null, doc._id )}")
class FrontEndMigrationDAOImpl(
	@Qualifier("baseCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig
) : GenericDAOImpl<FrontEndMigration>(FrontEndMigration::class.java, couchDbDispatcher, idGenerator, designDocumentProvider = designDocumentProvider, daoConfig = daoConfig), FrontEndMigrationDAO {

	@View(
		name = "by_userid_name",
		map = "function(doc) {\n" +
			"            if (doc.java_type == 'org.taktik.icure.entities.FrontEndMigration' && !doc.deleted && doc.name && doc.userId) {\n" +
			"            emit([doc.userId, doc.name],doc._id);\n" +
			"}\n" +
			"}"
	)
	override fun getFrontEndMigrationsByUserIdAndName(datastoreInformation: IDatastoreInformation, userId: String, name: String?) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = if (name == null) {
			// This is a range query
			val startKey = ComplexKey.of(userId)
			val endKey = ComplexKey.of(userId, ComplexKey.emptyObject())

			createQuery(datastoreInformation, "by_userid_name").startKey(startKey).endKey(endKey).includeDocs(true)
		} else {
			createQuery(datastoreInformation, "by_userid_name").key(ComplexKey.of(userId, name)).includeDocs(true)
		}
		emitAll(client.queryViewIncludeDocs<ComplexKey, String, FrontEndMigration>(viewQuery).map { it.doc })
	}
}
