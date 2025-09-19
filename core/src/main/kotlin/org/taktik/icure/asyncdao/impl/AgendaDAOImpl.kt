/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository
import org.taktik.couchdb.annotation.View
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.ViewQuery
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.queryViewIncludeDocsNoValue
import org.taktik.couchdb.queryViewNoValue
import org.taktik.icure.asyncdao.AgendaDAO
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.MAURICE_PARTITION
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.cache.ConfiguredCacheProvider
import org.taktik.icure.cache.getConfiguredCache
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.utils.distinct

@Repository("AgendaDAO")
@Profile("app")
@View(name = "all", map = "function(doc) { if (doc.java_type == 'org.taktik.icure.entities.Agenda' && !doc.deleted) emit( null, doc._id )}")
class AgendaDAOImpl(
	@Qualifier("healthdataCouchDbDispatcher") couchDbDispatcher: CouchDbDispatcher,
	idGenerator: IDGenerator,
	entityCacheFactory: ConfiguredCacheProvider,
	designDocumentProvider: DesignDocumentProvider,
	daoConfig: DaoConfig,
) : GenericDAOImpl<Agenda>(
	Agenda::class.java,
	couchDbDispatcher,
	idGenerator,
	entityCacheFactory.getConfiguredCache(),
	designDocumentProvider,
	daoConfig = daoConfig,
),
	AgendaDAO {
	@View(name = "by_user", map = "classpath:js/agenda/By_user.js")
	override fun getAgendasByUser(
		datastoreInformation: IDatastoreInformation,
		userId: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery =
			createQuery(datastoreInformation, "by_user")
				.startKey(userId)
				.endKey(userId)
				.includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<String, Agenda>(viewQuery).map { it.doc })
	}

	override fun listAgendaIdsByUser(
		datastoreInformation: IDatastoreInformation,
		userId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery =
			createQuery(datastoreInformation, "by_user")
				.startKey(userId)
				.endKey(userId)
				.includeDocs(false)

		emitAll(client.queryView<String, String>(viewQuery).mapNotNull { it.id })
	}

	@View(name = "readable_by_user", map = "classpath:js/agenda/Readable_by_user.js")
	override fun getReadableAgendaByUserLegacy(
		datastoreInformation: IDatastoreInformation,
		userId: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery =
			createQuery(datastoreInformation, "readable_by_user")
				.startKey(userId)
				.endKey(userId)
				.includeDocs(true)

		emitAll(client.queryViewIncludeDocsNoValue<String, Agenda>(viewQuery).map { it.doc })
	}

	override fun listReadableAgendaByUserLegacy(
		datastoreInformation: IDatastoreInformation,
		userId: String,
	) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery =
			createQuery(datastoreInformation, "readable_by_user")
				.startKey(userId)
				.endKey(userId)
				.includeDocs(false)

		emitAll(client.queryViewNoValue<String>(viewQuery).map { it.id })
	}

	@View(name = "readable_by_user_rights", map = "classpath:js/agenda/Readable_by_user_rights.js", secondaryPartition = MAURICE_PARTITION)
	override fun listReadableAgendaIdsByUserRights(
		datastoreInformation: IDatastoreInformation,
		userId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery =
			createQuery(datastoreInformation, "readable_by_user_rights", MAURICE_PARTITION)
				.startKey(userId)
				.endKey(userId)
				.includeDocs(false)

		emitAll(client.queryView<String, String>(viewQuery).map { it.id })
	}

	override fun listAgendasByTypedProperty(
		datastoreInformation: IDatastoreInformation,
		property: PropertyStub,
	): Flow<Agenda> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(
			client.queryViewIncludeDocsNoValue<ComplexKey, Agenda>(viewQueryByTypedProperty(datastoreInformation, property, true)).map {
				it.doc
			},
		)
	}

	@View(name = "by_typed_property", map = "classpath:js/agenda/By_typed_property.js", secondaryPartition = MAURICE_PARTITION)
	override fun listAgendasIdsByTypedProperty(
		datastoreInformation: IDatastoreInformation,
		property: PropertyStub,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryViewNoValue<ComplexKey>(viewQueryByTypedProperty(datastoreInformation, property, false)).map { it.id })
	}

	private suspend fun viewQueryByTypedProperty(
		datastoreInformation: IDatastoreInformation,
		property: PropertyStub,
		includeDocs: Boolean,
	): ViewQuery {
		val propertyId =
			requireNotNull(property.id) {
				"Cannot use a property with null id as key"
			}

		val key =
			listOfNotNull(
				property.typedValue?.stringValue?.let { ComplexKey.of(propertyId, "s", it) },
				property.typedValue?.booleanValue?.let { ComplexKey.of(propertyId, "b", it) },
				property.typedValue?.integerValue?.let { ComplexKey.of(propertyId, "i", it) },
				property.typedValue?.doubleValue?.let { ComplexKey.of(propertyId, "d", it) },
			).also {
				require(it.size == 1) { "Key property must have only one non-null value" }
			}.first()

		return createQuery(datastoreInformation, "by_typed_property", MAURICE_PARTITION)
			.key(key)
			.includeDocs(includeDocs)
	}

	override fun listAgendasWithProperty(
		datastoreInformation: IDatastoreInformation,
		propertyId: String,
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQuery =
			createQuery(datastoreInformation, "by_typed_property", MAURICE_PARTITION)
				.startKey(ComplexKey.of(propertyId, null, null))
				.endKey(ComplexKey.of(propertyId, ComplexKey.emptyObject(), ComplexKey.emptyObject()))
				.includeDocs(false)

		emitAll(client.queryView<ComplexKey, Void>(viewQuery).map { it.id }.distinct())
	}

	override suspend fun warmupPartition(
		datastoreInformation: IDatastoreInformation,
		partition: Partitions
	) {
		when (partition) {
			Partitions.Maurice -> warmup(datastoreInformation, "by_typed_property" to MAURICE_PARTITION)
			else -> super.warmupPartition(datastoreInformation, partition)
		}
	}
}
