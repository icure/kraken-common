/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import org.apache.commons.lang3.ArrayUtils
import org.slf4j.LoggerFactory
import org.taktik.couchdb.Client
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.create
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.dao.designDocName
import org.taktik.couchdb.entity.Attachment
import org.taktik.couchdb.entity.DesignDocument
import org.taktik.couchdb.entity.Option
import org.taktik.couchdb.entity.View
import org.taktik.couchdb.entity.ViewQuery
import org.taktik.couchdb.exception.CouchDbConflictException
import org.taktik.couchdb.exception.CouchDbException
import org.taktik.couchdb.exception.DocumentNotFoundException
import org.taktik.couchdb.exception.UpdateConflictException
import org.taktik.couchdb.get
import org.taktik.couchdb.id.IDGenerator
import org.taktik.couchdb.queryView
import org.taktik.couchdb.update
import org.taktik.icure.asyncdao.CouchDbDispatcher
import org.taktik.icure.asyncdao.GenericDAO
import org.taktik.icure.asyncdao.Partitions
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asyncdao.results.toBulkSaveResultFailure
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.exceptions.BulkUpdateConflictException
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.PersistenceException
import org.taktik.icure.utils.ViewQueries
import org.taktik.icure.utils.createPagedQueries
import org.taktik.icure.utils.createQueries
import org.taktik.icure.utils.createQuery
import org.taktik.icure.utils.pagedViewQuery
import org.taktik.icure.utils.pagedViewQueryOfIds
import org.taktik.icure.utils.suspendRetryForSomeException
import java.util.concurrent.CancellationException

abstract class GenericDAOImpl<T : StoredDocument>(
	protected val entityClass: Class<T>,
	protected val couchDbDispatcher: CouchDbDispatcher,
	protected val idGenerator: IDGenerator,
	protected val cacheChain: EntityCacheChainLink<T>? = null,
	private val designDocumentProvider: DesignDocumentProvider
) : GenericDAO<T> {
	private val log = LoggerFactory.getLogger(this.javaClass)

	override fun <K> getAllPaginated(datastoreInformation: IDatastoreInformation, offset: PaginationOffset<K>, keyClass: Class<K>): Flow<ViewQueryResultEvent> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val viewQuery = pagedViewQuery(
			datastoreInformation, "all", null, null, offset, false
		)
		emitAll(client.queryView(viewQuery, keyClass, String::class.java, entityClass))
	}

	override suspend fun contains(datastoreInformation: IDatastoreInformation, id: String): Boolean {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".contains: " + id)
		}
		return client.get(id, entityClass) != null
	}

	override suspend fun hasAny(datastoreInformation: IDatastoreInformation): Boolean {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return designDocContainsAllView(datastoreInformation) && client.queryView<String, String>(createQuery(
			datastoreInformation,
			"all"
		).limit(1)).count() > 0
	}

	/**
	 * Checks if the "all" view for the specified entity type exists on the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @return true if the view exists, false otherwise.
	 */
	private suspend fun designDocContainsAllView(datastoreInformation: IDatastoreInformation): Boolean {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		return client.get<DesignDocument>(
			designDocumentProvider.currentOrAvailableDesignDocumentId(client, entityClass, this)
		)?.views?.containsKey("all") ?: false
	}

	override fun getEntityIds(datastoreInformation: IDatastoreInformation, limit: Int?): Flow<String> = flow {
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".getAllIds")
		}
		if (designDocContainsAllView(datastoreInformation)) {
			val client = couchDbDispatcher.getClient(datastoreInformation)
			client.queryView<String, String>(if (limit != null) createQuery(datastoreInformation, "all").limit(limit) else createQuery(
				datastoreInformation,
				"all"
			)).onEach { emit(it.id) }.collect()
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun getEntities(datastoreInformation: IDatastoreInformation) = flow {
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".getAll")
		}
		val client = couchDbDispatcher.getClient(datastoreInformation)
		emitAll(client.queryView(createQuery(datastoreInformation, "all").includeDocs(true), Nothing::class.java, String::class.java, entityClass).map { (it as? ViewRowWithDoc<*, *, T?>)?.doc }.filterNotNull())
	}

	override suspend fun get(datastoreInformation: IDatastoreInformation, id: String, vararg options: Option): T? = get(datastoreInformation, id, null, *options)

	override suspend fun get(datastoreInformation: IDatastoreInformation, id: String, rev: String?, vararg options: Option): T? {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".get: " + id + " [" + ArrayUtils.toString(options) + "]")
		}
		return try {
			cacheChain?.getEntity(datastoreInformation.getFullIdFor(id))
				?: rev?.let { client.get(id, it, entityClass, *options) }?.also { cacheChain?.putInCache(datastoreInformation.getFullIdFor(id), it) }
				?: client.get(id, entityClass, *options)?.let {
					cacheChain?.putInCache(datastoreInformation.getFullIdFor(id), it)
					postLoad(datastoreInformation, it)
				}
		} catch (e: DocumentNotFoundException) {
			log.warn("Document not found", e)
			null
		}
	}

	override fun getEntities(datastoreInformation: IDatastoreInformation, ids: Collection<String>): Flow<T> =
		getEntities(datastoreInformation, ids.asFlow())

	override fun getEntities(datastoreInformation: IDatastoreInformation, ids: Flow<String>): Flow<T> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".get: " + ids)
		}

		ids.fold(emptyList<String>()) { nonCachedBatch, it ->
			cacheChain?.getEntity(datastoreInformation.getFullIdFor(it))?.let { e ->
				if(nonCachedBatch.isNotEmpty()) {
					emitAll(client.get(nonCachedBatch, entityClass).map {
						cacheChain.putInCache(datastoreInformation.getFullIdFor(it.id), it)
						this@GenericDAOImpl.postLoad(datastoreInformation, it)
					})
				}
				emit(e)
				emptyList()
			} ?: (nonCachedBatch + it)
		}.takeIf { it.isNotEmpty() }?.also { notEmitted ->
			emitAll(client.get(notEmitted, entityClass).map {
				cacheChain?.putInCache(datastoreInformation.getFullIdFor(it.id), it)
				this@GenericDAOImpl.postLoad(datastoreInformation, it)
			})
		}
	}

	override suspend fun create(datastoreInformation: IDatastoreInformation, entity: T): T? {
		return save(datastoreInformation, true, entity)
	}

	override suspend fun save(datastoreInformation: IDatastoreInformation, entity: T): T? {
		return save(datastoreInformation, null, entity)
	}

	/**
	 * Creates or updates an entity on the database. It creates one if the newEntity parameter is set to true or if the
	 * rev field of the entity passed as parameter is null, otherwise tries to update the entity. If the cache is not
	 * null, then the created/updated entity is also saved at all the level of the cache.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param newEntity if true, it will create a new entity, otherwise it will update it. It will create the entity also if its rev field is null.
	 * @param entity the entity to create or update.
	 * @return the updated entity or null.
	 */
	protected open suspend fun save(datastoreInformation: IDatastoreInformation, newEntity: Boolean?, entity: T): T? {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".save: " + entity.id + ":" + entity.rev)
		}

		try {
			return beforeSave(datastoreInformation, entity).let { e ->
				if (newEntity ?: (e.rev == null)) {
					client.create(e, entityClass)
				} else {
					client.update(e, entityClass)
					//saveRevHistory(entity, null);
				}.let {
					cacheChain?.putInCache(datastoreInformation.getFullIdFor(it.id), it)
					afterSave(datastoreInformation, it, e)
				}
			}
		} catch (e: CouchDbException) {
			cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(entity.id))
			throw e
		}

	}

	/**
	 * This operation is performed before saving an entity to the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity that is about to be saved.
	 * @return the entity after the operation.
	 */
	protected open suspend fun beforeSave(datastoreInformation: IDatastoreInformation, entity: T): T {
		return entity
	}

	/**
	 * This operation is performed after saving an entity to the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param savedEntity the entity [T] saved to the database.
	 * @param preSaveEntity the entity [T] before it was stored on the db.
	 * @return the entity after the operation.
	 */
	protected open suspend fun afterSave(datastoreInformation: IDatastoreInformation, savedEntity: T, preSaveEntity: T): T {
		return savedEntity
	}

	/**
	 * This operation is performed before deleting an entity to the database.
	 * Important: if you override this you should also override the
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity that is about to be deleted.
	 * @return the entity after the operation.
	 */
	protected open suspend fun beforeDelete(datastoreInformation: IDatastoreInformation, entity: T): T {
		return entity
	}

	/**
	 * This operation is performed after deleted an entity to the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the deleted entity.
	 * @return the entity after the operation.
	 */
	protected open suspend fun afterDelete(datastoreInformation: IDatastoreInformation, entity: T): T {
		return entity
	}

	/**
	 * This operation is performed before undeleting an entity on the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity that is about to be undeleted.
	 * @return the entity after the operation.
	 */
	protected open suspend fun beforeUnDelete(datastoreInformation: IDatastoreInformation, entity: T): T {
		return entity
	}

	/**
	 * This operation is performed after undeleting an entity on the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity that is about to be undeleted.
	 * @return the entity after the operation.
	 */
	protected open suspend fun afterUnDelete(datastoreInformation: IDatastoreInformation, entity: T): T {
		return entity
	}

	/**
	 * This operation is performed after loading an entity from the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the loaded entity.
	 * @return the entity after the operation.
	 */
	protected open suspend fun postLoad(datastoreInformation: IDatastoreInformation, entity: T): T {
		return entity
	}

	@Suppress("UNCHECKED_CAST")
	override suspend fun remove(datastoreInformation: IDatastoreInformation, entity: T): DocIdentifier {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".remove: " + entity)
		}
		val deleted = afterDelete(
			datastoreInformation,
			client.update(beforeDelete(datastoreInformation, entity).withDeletionDate(deletionDate = System.currentTimeMillis()) as T, entityClass)
		)
		cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(entity.id))
		return DocIdentifier(deleted.id, deleted.rev)
	}

	@Suppress("UNCHECKED_CAST")
	override suspend fun unRemove(datastoreInformation: IDatastoreInformation, entity: T): DocIdentifier {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".remove: " + entity)
		}
		// Before remove
		return beforeUnDelete(datastoreInformation, entity).let { e ->
			val undeleted = client.update(e.withDeletionDate(null) as T, entityClass).let {
				cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(it.id))
				afterUnDelete(datastoreInformation, it)
			}

			DocIdentifier(undeleted.id, undeleted.rev)
		}
	}

	override suspend fun purge(datastoreInformation: IDatastoreInformation, entity: T): DocIdentifier {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".remove: " + entity)
		}
		// Delete
		val purged = client.delete(beforeDelete(datastoreInformation, entity))
		// After remove
		cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(entity.id))
		afterDelete(datastoreInformation, entity)
		return purged
	}

	@Suppress("UNCHECKED_CAST")
	override fun remove(datastoreInformation: IDatastoreInformation, entities: Collection<T>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug("remove $entities")
		}
		try {
			val toBeDeletedEntities = entities.map { entity ->
				beforeDelete(datastoreInformation, entity).let {
					it.withDeletionDate(System.currentTimeMillis()) as T
				}
			}
			val bulkUpdateResults = client.bulkUpdate(toBeDeletedEntities, entityClass).map { r ->
				toBeDeletedEntities.firstOrNull { e -> r.id == e.id }?.let {
					cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(it.id))
					afterDelete(datastoreInformation, it.withIdRev(rev = r.rev!!) as T)
				}
				DocIdentifier(r.id, r.rev)
			}
			emitAll(bulkUpdateResults)
		} catch (e: Exception) {
			if (e !is CancellationException) throw PersistenceException("failed to remove entities ", e)
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun unRemove(datastoreInformation: IDatastoreInformation, entities: Collection<T>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug("remove $entities")
		}
		try {
			val bulkUpdateResults = client.bulkUpdate(
				entities.map { entity ->
					beforeUnDelete(datastoreInformation, entity).let {
						it.withDeletionDate(null) as T
					}
				},
				entityClass
			).onEach { r ->
				entities.firstOrNull { e -> r.id == e.id }?.let {
					cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(it.id))
					afterUnDelete(datastoreInformation, it)
				}
			}
			emitAll(bulkUpdateResults.map { DocIdentifier(it.id, it.rev) })
		} catch (e: Exception) {
			throw PersistenceException("failed to remove entities ", e)
		}
	}

	override fun <K : Collection<T>> create(datastoreInformation: IDatastoreInformation, entities: K): Flow<T> {
		return save(datastoreInformation, true, entities)
	}

	override fun <K : Collection<T>> save(datastoreInformation: IDatastoreInformation, entities: K): Flow<T> {
		return save(datastoreInformation, false, entities)
	}

	@Suppress("UNCHECKED_CAST")
	override fun saveBulk(
		datastoreInformation: IDatastoreInformation,
		entities: Collection<T>
	): Flow<BulkSaveResult<T>> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".saveBulk: " + entities.map { entity -> entity.id + ":" + entity.rev })
		}

		// Save entity
		val fixedEntities = entities.map {
			cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(it.id))
			beforeSave(datastoreInformation, it)
		}
		val fixedEntitiesById = entities.associateBy { it.id }

		emitAll(
			client.bulkUpdate(fixedEntities, entityClass).map { updateResult ->
				updateResult.rev?.let { newRev ->
					fixedEntitiesById.getValue(updateResult.id).withIdRev(rev = newRev) as T
				}?.let {
					cacheChain?.putInCache(datastoreInformation.getFullIdFor(it.id), it)
					BulkSaveResult.Success(afterSave(datastoreInformation, it, fixedEntitiesById.getValue(it.id)))
				}
					?: updateResult.toBulkSaveResultFailure()
					?: throw IllegalStateException("Received an unsuccessful bulk update result without error from couchdb")
			}
		)
	}

	// TODO SH later: make sure this is correct
	/**
	 * Creates or updates a collection of new entities on the database. If there is a cache, then the created entities
	 * are also saved at all levels of the cache. If an entity already exists, but it has a wrong rev, than it will not
	 * be created or updated and will be ignored in the final result.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param newEntity whether the entities passed as parameter are new or not.
	 * @param entities the [Collection] of entities to create.
	 * @return a [Flow] containing the created entities.
	 */
	@Suppress("UNCHECKED_CAST")
	protected open fun <K : Collection<T>> save(datastoreInformation: IDatastoreInformation, newEntity: Boolean?, entities: K): Flow<T> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".save: " + entities.mapNotNull { entity -> entity.id + ":" + entity.rev })
		}

		// Save entity
		val fixedEntities = entities.map {
			cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(it.id))
			beforeSave(datastoreInformation, it)
		}
		val fixedEntitiesById = fixedEntities.associateBy { it.id }

		val results = client.bulkUpdate(fixedEntities, entityClass).toList() //Attachment dirty has been lost

		val conflicts = results.filter { it.error == "conflict" }.map { r -> UpdateConflictException(r.id, r.rev ?: "unknown") }.toList()
		if (conflicts.isNotEmpty()) {
			throw BulkUpdateConflictException(conflicts, fixedEntities)
		}
		emitAll(
			results.asFlow().mapNotNull { r ->
				fixedEntitiesById.getValue(r.id).let {
					r.rev?.let { newRev ->
						it.withIdRev(rev = newRev) as T
					}
				}
			}.map {
				afterSave(datastoreInformation, it, fixedEntitiesById.getValue(it.id)).also { saved ->
					cacheChain?.putInCache(datastoreInformation.getFullIdFor(saved.id), saved)
				}
			}
		)
	}

	override suspend fun forceInitStandardDesignDocument(
		datastoreInformation: IDatastoreInformation,
		updateIfExists: Boolean,
		dryRun: Boolean,
		partition: Partitions
	): List<DesignDocument> = forceInitStandardDesignDocument(couchDbDispatcher.getClient(datastoreInformation), updateIfExists, dryRun, partition)

	override suspend fun forceInitStandardDesignDocument(
		client: Client,
		updateIfExists: Boolean,
		dryRun: Boolean,
		partition: Partitions
	): List<DesignDocument> {
		val generatedDocs = designDocumentProvider.generateDesignDocuments(this.entityClass, this, client, partition)
		return generatedDocs.mapNotNull { generated ->
			suspendRetryForSomeException<DesignDocument?, CouchDbConflictException>(3) {
				val fromDatabase = client.get(generated.id, DesignDocument::class.java)
				val (merged, changed) = fromDatabase?.mergeWith(generated, true) ?: (generated to true)
				if (changed && (updateIfExists || fromDatabase == null)) {
					if (!dryRun) {
						try {
							fromDatabase?.let {
								client.update(merged.copy(rev = it.rev))
							} ?: client.create(merged)
						} catch (e: CouchDbConflictException) {
							log.error("Cannot create DD: ${merged.id} with revision ${merged.rev}")
							throw e
						}
					} else {
						merged
					}
				} else null
			}
		}
	}

	override suspend fun initSystemDocumentIfAbsent(datastoreInformation: IDatastoreInformation) {
		initSystemDocumentIfAbsent(couchDbDispatcher.getClient(datastoreInformation))
	}

	override suspend fun initSystemDocumentIfAbsent(client: Client) {
		val designDocId = designDocName("_System")
		val designDocument = client.get(designDocId, DesignDocument::class.java)
		if (designDocument == null) {
			client.create(DesignDocument(designDocId, views = mapOf("revs" to View("function (doc) { emit(doc.java_type, doc._rev); }"))))
		}
	}

	/**
	 * Get the existing attachments stubs for an entity. It is important to always include the attachment stubs existing
	 * on the entity before updating it as if we don't include them couchdb will delete them.
	 *
	 * It is possible to exclude one attachment id from the returned map, which allows to easily delete a document
	 * attachment.
	 */
	protected suspend fun getExistingEntityAttachmentStubs(
		datastoreInformation: IDatastoreInformation,
		entity: T,
		excludingAttachmentId: String? = null
	): Map<String, Attachment>? =
		if (entity.rev == null)
			null
		else
			(
					get(datastoreInformation, entity.id) ?: throw ConflictRequestException(
						"Entity with id ${entity.id} not found, but was expected to exist with revision ${entity.rev}"
					)
					).attachments?.let { oldAttachments ->
					excludingAttachmentId?.let { oldAttachmentId ->
						oldAttachments - oldAttachmentId
					} ?: oldAttachments
				}

	protected suspend fun createQuery(
		datastoreInformation: IDatastoreInformation,
		viewName: String,
		secondaryPartition: String? = null
	): ViewQuery =
		designDocumentProvider.createQuery(couchDbDispatcher.getClient(datastoreInformation), this, viewName, entityClass, secondaryPartition)

	protected suspend fun createQueries(datastoreInformation: IDatastoreInformation, vararg viewQueries: Pair<String, String?>) =
		designDocumentProvider.createQueries(couchDbDispatcher.getClient(datastoreInformation), this, entityClass, *viewQueries)

	protected suspend fun createQueries(datastoreInformation: IDatastoreInformation,viewQueryOnMain: String, viewQueryOnSecondary: Pair<String, String?>) =
		designDocumentProvider.createQueries(couchDbDispatcher.getClient(datastoreInformation), this, entityClass, viewQueryOnMain, viewQueryOnSecondary)


	protected suspend fun <P> pagedViewQuery(datastoreInformation: IDatastoreInformation, viewName: String, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean, secondaryPartition: String? = null): ViewQuery =
		designDocumentProvider.pagedViewQuery(couchDbDispatcher.getClient(datastoreInformation), this, viewName, entityClass, startKey, endKey, pagination, descending, secondaryPartition)

	protected suspend fun <P> createPagedQueries(datastoreInformation: IDatastoreInformation, viewQueryOnMain: String, viewQueryOnSecondary: Pair<String, String?>, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean) =
		designDocumentProvider.createPagedQueries(couchDbDispatcher.getClient(datastoreInformation), this, entityClass, viewQueryOnMain, viewQueryOnSecondary, startKey, endKey, pagination, descending)

	protected suspend fun <P> createPagedQueries(datastoreInformation: IDatastoreInformation, viewQueries: List<Pair<String, String?>>, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean): ViewQueries =
		designDocumentProvider.createPagedQueries(couchDbDispatcher.getClient(datastoreInformation), this, entityClass, viewQueries, startKey, endKey, pagination, descending)

	protected suspend fun <P> pagedViewQueryOfIds(datastoreInformation: IDatastoreInformation, viewName: String, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, secondaryPartition: String? = null) =
		designDocumentProvider.pagedViewQueryOfIds(couchDbDispatcher.getClient(datastoreInformation), this, viewName, entityClass, startKey, endKey, pagination, secondaryPartition)
}
