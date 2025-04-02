/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.apache.commons.lang3.ArrayUtils
import org.slf4j.LoggerFactory
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.create
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.entity.DesignDocument
import org.taktik.couchdb.entity.Option
import org.taktik.couchdb.entity.ViewQuery
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.update
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.InternalDAO
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asyncdao.results.toBulkSaveResultFailure
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.utils.ViewQueries
import org.taktik.icure.utils.createPagedQueries
import org.taktik.icure.utils.createQueries
import org.taktik.icure.utils.createQuery
import org.taktik.icure.utils.pagedViewQuery
import org.taktik.icure.utils.pagedViewQueryOfIds

open class InternalDAOImpl<T : StoredDocument>(
	val entityClass: Class<T>,
	val couchDbDispatcher: CouchDbDispatcher,
	val idGenerator: IDGenerator,
	val datastoreInstanceProvider: DatastoreInstanceProvider,
	val designDocumentProvider: DesignDocumentProvider
) : InternalDAO<T> {
	private val log = LoggerFactory.getLogger(javaClass)
	//private val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())

	@Suppress("UNCHECKED_CAST")
	override fun getEntities(): Flow<T> = flow {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		emitAll(
			client.queryView(
				ViewQuery()
					.designDocId(designDocumentProvider.currentOrAvailableDesignDocumentId(client, entityClass, this@InternalDAOImpl))
					.viewName("all")
					.includeDocs(true),
				String::class.java, String::class.java, entityClass
			).map { (it as? ViewRowWithDoc<*, *, T?>)?.doc }.filterNotNull()
		)
	}

	override fun getEntityIds(): Flow<String> = flow {
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".getAllIds")
		}
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		emitAll(
			client.queryView<String, String>(
				ViewQuery()
					.designDocId(designDocumentProvider.currentOrAvailableDesignDocumentId(client, entityClass, this@InternalDAOImpl))
					.viewName("all")
					.includeDocs(false)
			).map { it.id }.filterNotNull()
		)
	}

	override fun getEntityIdsPaginated(paginationOffset: PaginationOffset<Nothing>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		val viewQuery = designDocumentProvider.pagedViewQueryOfIds(
				client = client,
				metadataSource = this@InternalDAOImpl,
				viewName = "all",
				entityClass = entityClass,
				startKey = null,
				endKey = null,
				pagination = paginationOffset
			)
		emitAll(client.queryView(viewQuery, Any::class.java, String::class.java, Any::class.java))
	}

	override suspend fun get(id: String, vararg options: Option): T? = get(id, null, *options)

	override suspend fun get(id: String, rev: String?, vararg options: Option): T? {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".get: " + id + " [" + ArrayUtils.toString(options) + "]")
		}
		return try {
			return rev?.let { client.get(id, entityClass, *options) } ?: client.get(id, entityClass, *options)
		} catch (e: DocumentNotFoundException) {
			log.warn("Document not found", e)
			null
		}
	}

	override fun getEntities(ids: Collection<String>): Flow<T> = flow {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".get: " + ids)
		}
		emitAll(client.get(ids, entityClass))
	}

	override suspend fun save(entity: T): T? {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".save: " + entity.id + ":" + entity.rev)
		}
		return when {
			entity.rev == null -> {
				client.create(entity, entityClass)
			}
			else -> {
				client.update(entity, entityClass)
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun save(entities: Flow<T>): Flow<BulkSaveResult<T>> = flow {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".save flow of entities")
		}
		val entitiesById = entities.toList().associateBy { it.id }
		emitAll(
			client.bulkUpdate(entitiesById.values, entityClass).map { updateResult ->
				if (updateResult.ok == true) {
					val updatedEntity = entitiesById.getValue(updateResult.id).withIdRev(
						rev = checkNotNull(updateResult.rev) { "Updated was successful but rev is null" }
					) as T
					BulkSaveResult.Success(updatedEntity)
				} else {
					updateResult.toBulkSaveResultFailure()
						?: throw IllegalStateException("Received an unsuccessful bulk update result without error from couchdb")
				}
			}
		)
	}

	override fun save(entities: List<T>): Flow<BulkSaveResult<T>> = save(entities.asFlow())

	override suspend fun update(entity: T): T? {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".save: " + entity.id + ":" + entity.rev)
		}
		return client.update(entity, entityClass)
	}

	override suspend fun purge(entity: T): DocIdentifier {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".purge: " + entity)
		}
		return client.delete(entity)
	}

	override fun purge(entities: Flow<T>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".purge flow of entities ")
		}
		emitAll(client.bulkDelete(entities.toList()))
	}

	override suspend fun remove(entity: T): T {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".remove: " + entity)
		}
		@Suppress("UNCHECKED_CAST")
		return client.update(entity.withDeletionDate(deletionDate = System.currentTimeMillis()) as T, entityClass)
	}

	@Suppress("UNCHECKED_CAST")
	override fun remove(entities: Flow<T>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".remove flow of entities ")
		}
		emitAll(client
			.bulkUpdate(entities.map { it.withDeletionDate(System.currentTimeMillis()) as T }.toList(), entityClass)
		)
	}

	//
	override suspend fun forceInitStandardDesignDocument(updateIfExists: Boolean) {
		val client = couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup())
		val generateds = designDocumentProvider.generateDesignDocuments(this.entityClass, this)

		generateds.forEach { generated ->
			val fromDatabase = client.get(generated.id, DesignDocument::class.java)
			val (merged, changed) = fromDatabase?.mergeWith(generated, true) ?: (generated to true)
			if (changed && updateIfExists) {
				fromDatabase?.let {
					client.update(merged.copy(rev = it.rev))
				} ?: client.create(merged)
			}
		}
	}

	protected suspend fun createQuery(viewName: String, secondaryPartition: String? = null): ViewQuery =
		designDocumentProvider.createQuery(couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup()),this, viewName, entityClass, secondaryPartition)

	protected suspend fun createQueries(vararg viewQueries: Pair<String, String?>) =
		designDocumentProvider.createQueries(couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup()),this, entityClass, true, *viewQueries)

	protected suspend fun createQueries(viewQueryOnMain: String, viewQueryOnSecondary: Pair<String, String?>) =
		designDocumentProvider.createQueries(couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup()),this, entityClass, viewQueryOnMain, viewQueryOnSecondary, true)


	protected suspend fun <P> pagedViewQuery(viewName: String, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean, secondaryPartition: String? = null): ViewQuery =
		designDocumentProvider.pagedViewQuery(couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup()),this, viewName, entityClass, startKey, endKey, pagination, descending, secondaryPartition)

	protected suspend fun <P> createPagedQueries(viewQueryOnMain: String, viewQueryOnSecondary: Pair<String, String?>, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean) =
		designDocumentProvider.createPagedQueries(couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup()),this, entityClass, viewQueryOnMain, viewQueryOnSecondary, startKey, endKey, pagination, descending, true)

	protected suspend fun <P> createPagedQueries(viewQueries: List<Pair<String, String?>>, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean): ViewQueries =
		designDocumentProvider.createPagedQueries(couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup()),this, entityClass, viewQueries, startKey, endKey, pagination, descending, true)

	protected suspend fun <P> pagedViewQueryOfIds(viewName: String, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, secondaryPartition: String? = null) =
		designDocumentProvider.pagedViewQueryOfIds(couchDbDispatcher.getClient(datastoreInstanceProvider.getInstanceAndGroup()),this, viewName, entityClass, startKey, endKey, pagination, secondaryPartition)
}
