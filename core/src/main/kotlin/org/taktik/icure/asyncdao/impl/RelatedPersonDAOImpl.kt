/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.EmptyObjectKey
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.DATA_OWNER_PARTITION
import org.taktik.icure.asyncdao.RelatedPersonDAO
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.dao.QueryProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.utils.distinct

@Repository("relatedPersonDAO")
@Profile("app")
@View(
	name = "all",
	map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.RelatedPerson' && !doc.deleted) emit( null, doc._id )}",
)
internal class RelatedPersonDAOImpl(
	@Qualifier("patientCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
	queryProvider: QueryProvider,
) : ConflictDAOImpl<RelatedPerson>(
	entityClass = RelatedPerson::class.java,
	couchDbDispatcher = couchDbDispatcher,
	idGenerator = idGenerator,
	cacheChain = entityCacheFactory.getConfiguredCache(),
	designDocumentProvider = designDocumentProvider,
	daoConfig = daoConfig,
	queryProvider = queryProvider,
), RelatedPersonDAO {

	@View(
		name = "conflicts",
		map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.RelatedPerson' && !doc.deleted && doc._conflicts) emit(doc._id) }",
	)
	override fun listConflicts(datastoreInformation: IDatastoreInformation) =
		doListConflicts<RelatedPerson>(datastoreInformation, "conflicts", null)

	override fun listIdsOfEntitiesWithConflicts(datastoreInformation: IDatastoreInformation): Flow<String> =
		doListIdsOfEntitiesWithConflicts<RelatedPerson>(datastoreInformation, "conflicts", null)

	override fun findRelatedPersonsByIds(
		datastoreInformation: IDatastoreInformation,
		relatedPersonIds: Flow<String>,
	): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.getForPagination(relatedPersonIds, RelatedPerson::class.java))
	}

	@View(name = "by_data_owner", map = "classpath:js/relatedperson/By_data_owner_map.js", secondaryPartition = DATA_OWNER_PARTITION)
	override fun listRelatedPersonIdsByDataOwner(
		datastoreInformation: IDatastoreInformation,
		dataOwnerId: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = createQuery(
			datastoreInformation = datastoreInformation,
			legacyView = "by_data_owner" to DATA_OWNER_PARTITION,
			configurationView = "by_all_delegates",
		)
			.startKey(arrayOf(dataOwnerId))
			.endKey(arrayOf(dataOwnerId))
			.includeDocs(false)

		emitAll(client.queryView<Array<String>, Int>(viewQuery).map { it.id })
	}.distinct()

	@View(
		name = "by_data_owner_contains_name",
		map = "classpath:js/relatedperson/By_data_owner_contains_name_map.js",
		secondaryPartition = DATA_OWNER_PARTITION,
	)
	override fun listRelatedPersonIdsByDataOwnerNameContainsFuzzy(
		datastoreInformation: IDatastoreInformation,
		searchString: String?,
		dataOwnerId: String,
		limit: Int?,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val name = if (searchString != null) sanitizeString(searchString) else null
		val viewQuery = createQuery(
			datastoreInformation = datastoreInformation,
			legacyView = "by_data_owner_contains_name" to DATA_OWNER_PARTITION,
			configurationView = "by_all_delegates_contains_name",
		)
			.startKey(ComplexKey.of(dataOwnerId, name))
			.endKey(ComplexKey.of(dataOwnerId, name?.let { "$it\ufff0" } ?: EmptyObjectKey))
			.let { q -> limit?.let { q.limit(it) } ?: q }
			.includeDocs(false)

		emitAll(client.queryView<ComplexKey, String>(viewQuery).map { it.id })
	}.distinct()

	@View(
		name = "by_data_owner_identifier",
		map = "classpath:js/relatedperson/By_data_owner_identifier_map.js",
		secondaryPartition = DATA_OWNER_PARTITION,
	)
	override fun listRelatedPersonIdsByDataOwnerAndIdentifiers(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		identifiers: List<Identifier>,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = identifiers.flatMap {
			searchKeys.map { key ->
				if (it.value != null) {
					ComplexKey.of(key, it.system, it.value)
				} else {
					ComplexKey.of(key, it.system)
				}
			}
		}

		val viewQuery = createQuery(
			datastoreInformation = datastoreInformation,
			legacyView = "by_data_owner_identifier" to DATA_OWNER_PARTITION,
			configurationView = "by_all_delegates_identifier",
		).keys(keys).includeDocs(false)

		emitAll(client.queryView<ComplexKey, Int>(viewQuery).map { it.id })
	}.distinct()
}
