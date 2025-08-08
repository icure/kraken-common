/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import org.apache.commons.lang3.ArrayUtils
import org.slf4j.LoggerFactory
import org.taktik.couchdb.BulkUpdateResult
import org.taktik.couchdb.Client
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.couchdb.create
import org.taktik.couchdb.dao.DesignDocumentProvider
import org.taktik.couchdb.dao.designDocName
import org.taktik.couchdb.entity.Attachment
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.DesignDocument
import org.taktik.couchdb.entity.EntityExceptionBehaviour
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
import org.taktik.icure.asyncdao.results.entityOrNull
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asyncdao.results.toBulkSaveResultFailure
import org.taktik.icure.cache.EntityCacheChainLink
import org.taktik.icure.config.DaoConfig
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.base.StoredDocument
import org.taktik.icure.entities.utils.ExternalFilterKey
import org.taktik.icure.exceptions.BulkUpdateConflictException
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.PersistenceException
import org.taktik.icure.utils.ViewQueries
import org.taktik.icure.utils.createPagedQueries
import org.taktik.icure.utils.createQueries
import org.taktik.icure.utils.createQuery
import org.taktik.icure.security.error
import org.taktik.icure.utils.interleave
import org.taktik.icure.utils.pagedViewQuery
import org.taktik.icure.utils.pagedViewQueryOfIds
import org.taktik.icure.utils.queryView
import org.taktik.icure.utils.suspendRetryForSomeException
import java.time.Duration
import java.util.*

abstract class GenericDAOImpl<T : StoredDocument>(
	override val entityClass: Class<T>,
	protected val couchDbDispatcher: CouchDbDispatcher,
	protected val idGenerator: IDGenerator,
	protected val cacheChain: EntityCacheChainLink<T>? = null,
	private val designDocumentProvider: DesignDocumentProvider,
	protected val daoConfig: DaoConfig
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
			cacheChain?.getEntity(datastoreInformation.getFullIdFor(id))?.takeIf { rev == null || it.rev == rev }
				?: rev?.let { client.get(id, it, entityClass, *options) }
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
		val allIds = LinkedList<String>()
		val toRetrieve = LinkedList<String>()
		val cached = LinkedList<T>()
		ids.collect { id ->
			val currCached = cacheChain?.getEntity(datastoreInformation.getFullIdFor(id))
			if (currCached != null) {
				cached.addLast(currCached)
			} else {
				toRetrieve.addLast(id)
			}
			allIds.addLast(id)
		}
		// TODO do we want to limit size of the request?
		if (toRetrieve.isNotEmpty()) {
			client.get(
				toRetrieve,
				entityClass,
				onEntityException = EntityExceptionBehaviour.Recover
			).collect { retrieved ->
				val postLoaded = this@GenericDAOImpl.postLoad(datastoreInformation, retrieved)
				cacheChain?.putInCache(datastoreInformation.getFullIdFor(postLoaded.id), postLoaded)
				while (retrieved.id != allIds.first()) {
					// Some entities may not exist or be of the wrong type, ignore while not matching a cached entity
					if (cached.firstOrNull()?.id == allIds.first()) {
						emit(cached.removeFirst())
					}
					allIds.removeFirst()
				}
				emit(retrieved)
				allIds.removeFirst()
			}
		}
		while (allIds.isNotEmpty() && cached.isNotEmpty()) {
			if (allIds.removeFirst() == cached.first().id) {
				emit(cached.removeFirst())
			}
		}
		check(cached.isEmpty()) { "Should have consumed all cached entities" }
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

	override fun purge(
		datastoreInformation: IDatastoreInformation,
		entities: Collection<T>
	): Flow<BulkSaveResult<DocIdentifier>> = flow {
		// Before purging the entity we do a "standard" delete. This helps with
		val removed = internalRemove(datastoreInformation, entities).filterSuccessfulUpdates().toList()
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".purge: " + entities)
		}
		val purged = client.bulkDelete(removed).toSaveResult { id, rev ->
			cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(id))
			DocIdentifier(id, rev)
		}.toList()
		if (removed.size != purged.size) {
			// The possibility of this happening should be effectively null: it should be impossible for someone to get
			// the removed entity and update it (going from couch, to kraken, to client, back to kraken and to couch)
			// before we finish this (going from couch to kraken back to couch).
			log.error {
				"iCure purging error: some entities where successfully removed but could not be purged after remove ${
					removed.map { it.id }.toSet() - purged.mapNotNull { it.entityOrNull()?.id }.toSet()
				}"
			}
		}
		purged.forEach { emit(it) }
	}

	protected fun <T> Flow<BulkUpdateResult>.toSaveResult(
		mapSuccess: suspend (id: String, rev: String) -> T
	): Flow<BulkSaveResult<T>> =
		map { r ->
			if (r.ok == true) {
				BulkSaveResult.Success(mapSuccess(r.id, r.rev!!))
			} else r.toBulkSaveResultFailure()
				?: throw IllegalStateException("Received an unsuccessful bulk update result without error from couchdb")
		}

	override fun remove(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<BulkSaveResult<T>> =
		internalRemove(datastoreInformation, entities)

	private fun internalRemove(datastoreInformation: IDatastoreInformation, entities: Collection<T>) = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug("Deleted $entities")
		}
		val toBeDeletedEntities = entities.map { entity ->
			beforeDelete(datastoreInformation, entity).let {
				@Suppress("UNCHECKED_CAST")
				it.withDeletionDate(System.currentTimeMillis()) as T
			}
		}
		val bulkUpdateResults = client.bulkUpdate(toBeDeletedEntities, entityClass).toSaveResult { id, rev ->
			toBeDeletedEntities.first { e -> id == e.id }.let {
				cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(it.id))
				@Suppress("UNCHECKED_CAST")
				afterDelete(datastoreInformation, it.withIdRev(rev = rev) as T)
			}
		}
		emitAll(bulkUpdateResults)
	}

	@Suppress("UNCHECKED_CAST")
	override fun unRemove(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<BulkSaveResult<T>> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug("Undeleted $entities")
		}
		try {
			val fixedEntitiesById = entities.map { entity ->
				beforeUnDelete(datastoreInformation, entity).let {
					it.withDeletionDate(null) as T
				}
			}.associateBy { it.id }
			val bulkUpdateResults = client.bulkUpdate(
				fixedEntitiesById.values,
				entityClass
			).toSaveResult { id, rev ->
				val fixedEntityWithNewRev = afterUnDelete(
					datastoreInformation,
					fixedEntitiesById.getValue(id).withIdRev(rev = rev) as T
				)
				cacheChain?.putInCache(datastoreInformation.getFullIdFor(id), fixedEntityWithNewRev)
				fixedEntityWithNewRev
			}
			emitAll(bulkUpdateResults)
		} catch (e: Exception) {
			throw PersistenceException("failed to remove entities ", e)
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <K : Collection<T>> create(datastoreInformation: IDatastoreInformation, entities: K): Flow<T> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		if (log.isDebugEnabled) {
			log.debug(entityClass.simpleName + ".create: " + entities.mapNotNull { entity -> entity.id + ":" + entity.rev })
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
				if (updateResult.ok == true) {
					val updatedEntity = fixedEntitiesById.getValue(updateResult.id).withIdRev(
						rev = checkNotNull(updateResult.rev) { "Updated was successful but rev is null" }
					) as T
					updatedEntity.let {
						cacheChain?.putInCache(datastoreInformation.getFullIdFor(it.id), it)
						BulkSaveResult.Success(afterSave(datastoreInformation, it, fixedEntitiesById.getValue(it.id)))
					}
				} else {
					updateResult.toBulkSaveResultFailure()
						?: throw IllegalStateException("Received an unsuccessful bulk update result without error from couchdb")
				}
			}
		)
	}

	override suspend fun forceInitExternalDesignDocument(
		datastoreInformation: IDatastoreInformation,
		partitionsWithRepo: Map<String, String>,
		updateIfExists: Boolean,
		dryRun: Boolean,
		ignoreIfUnchanged: Boolean
	): List<DesignDocument> {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val generatedDocs = designDocumentProvider.generateExternalDesignDocuments(
			entityClass = this.entityClass,
			partitionsWithRepo = partitionsWithRepo,
			client = client,
			ignoreIfUnchanged = ignoreIfUnchanged)
		return saveDesignDocs(generatedDocs, client, updateIfExists, dryRun)
	}

	override suspend fun forceInitStandardDesignDocument(
		datastoreInformation: IDatastoreInformation,
		updateIfExists: Boolean,
		dryRun: Boolean,
		partition: Partitions,
		ignoreIfUnchanged: Boolean
	): List<DesignDocument> = forceInitStandardDesignDocument(couchDbDispatcher.getClient(datastoreInformation), updateIfExists, dryRun, partition, ignoreIfUnchanged)

	override suspend fun forceInitStandardDesignDocument(
		client: Client,
		updateIfExists: Boolean,
		dryRun: Boolean,
		partition: Partitions,
		ignoreIfUnchanged: Boolean
	): List<DesignDocument> {
		val generatedDocs = designDocumentProvider.generateDesignDocuments(this.entityClass, this, client, partition, ignoreIfUnchanged)
		return saveDesignDocs(generatedDocs, client, updateIfExists, dryRun)
	}

	private suspend fun saveDesignDocs(
		generatedDocs: Set<DesignDocument>,
		client: Client,
		updateIfExists: Boolean,
		dryRun: Boolean
	) = generatedDocs.mapNotNull { generated ->
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
		else (get(datastoreInformation, entity.id) ?:
			throw ConflictRequestException("Entity with id ${entity.id} not found, but was expected to exist with revision ${entity.rev}")
		).attachments?.let { oldAttachments ->
			excludingAttachmentId?.let { oldAttachmentId ->
				oldAttachments - oldAttachmentId
			} ?: oldAttachments
		}

	/**
	 * Retrieves all the ids for an entity in the legacy and secure delegation views given a set of search keys and
	 * a set of secret foreign keys.
	 * This method will work if the provided partition emit a key that is [accessKey, secretForeignKey] and as value the
	 * date (as timestamp or fuzzy date).
	 */
	protected fun getEntityIdsByDataOwnerPatientDate(
		views: List<Pair<String, String>>,
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val keys = secretForeignKeys.flatMap { fk ->
			searchKeys.map { key -> ComplexKey.of(key, fk)}
		}.sortedWith(compareBy({ it.components[0] as String }, { it.components[1] as String }))

		val viewQueries = createQueries(
			datastoreInformation,
			*views.toTypedArray()
		).doNotIncludeDocs().keys(keys)

		client.interleave<ComplexKey, Long>(viewQueries, compareBy({ it.components[0] as String }, { it.components[1] as String }))
			.filterIsInstance<ViewRowNoDoc<ComplexKey, Long>>()
			.mapNotNull {
				if((it.value == null && startDate == null && endDate == null) || it.value !== null && (startDate == null || it.value!! >= startDate) && (endDate == null || it.value!! <= endDate)) {
					it.id to (it.value ?: 0)
				} else null
			}
			.toList()
			.sortedWith(if(descending) Comparator { o1, o2 ->
				o2.second.compareTo(o1.second).let {
					if(it == 0) o2.first.compareTo(o1.first) else it
				}
			} else compareBy({ it.second }, { it.first })
			)
			.forEach { emit(it.first) }
	}.distinctUntilChanged() // This works because ids will be sorted by date first

	override fun listEntitiesIdInCustomView(
		datastoreInformation: IDatastoreInformation,
		viewName: String,
		partitionName: String,
		startKey: ExternalFilterKey?,
		endKey: ExternalFilterKey?
	): Flow<String> = flow {
		val client = couchDbDispatcher.getClient(datastoreInformation)

		val query = createQuery(
			datastoreInformation = datastoreInformation,
			viewName = viewName,
			secondaryPartition = partitionName
		).includeDocs(false).let {
			if(startKey != null) it.startKey(startKey.key)
			else it
		}.let {
			if(endKey != null) it.endKey(endKey.key)
			else it
		}

		emitAll(client.queryView<Any?, Any?, Any?>(query)
			.filterIsInstance<ViewRowNoDoc<*, *>>()
			.map { it.id }
		)
	}

	protected suspend fun warmup(datastoreInformation: IDatastoreInformation, view: Pair<String, String?>) {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val viewQueries = designDocumentProvider
			.createQueries(client, this, entityClass, true, view)
			.keys(listOf(arrayOf("identifier")))
			.doNotIncludeDocs()
		client.interleave<Array<String>, String>(viewQueries, compareBy {it[0]}).firstOrNull()
	}

	override suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions) {
		when(partition) {
			Partitions.All -> {
				warmupPartition(datastoreInformation, Partitions.Main)
				warmupPartition(datastoreInformation, Partitions.DataOwner)
				warmupPartition(datastoreInformation, Partitions.Maurice)
			}
			Partitions.Main -> getEntityIds(datastoreInformation, 1).firstOrNull()
			else -> {}
		}
	}

	override suspend fun warmupExternalDesignDocs(
		datastoreInformation: IDatastoreInformation,
		designDocuments: List<DesignDocument>
	) = designDocuments.mapNotNull {
		if(it.id.contains("-")) {
			val (dd, partition) = it.id.split("-", limit = 2)
			if(dd.contains(entityClass.simpleName) && it.views.isNotEmpty()) {
				it.views.keys.first() to partition
			} else null
		} else null
	}.forEach {
		warmup(datastoreInformation, it)
	}

	protected suspend fun createQuery(
		datastoreInformation: IDatastoreInformation,
		viewName: String,
		secondaryPartition: String? = null
	): ViewQuery =
		designDocumentProvider.createQuery(couchDbDispatcher.getClient(datastoreInformation), this, viewName, entityClass, secondaryPartition)

	protected suspend fun createQueries(datastoreInformation: IDatastoreInformation, vararg viewQueries: Pair<String, String?>) =
		designDocumentProvider.createQueries(couchDbDispatcher.getClient(datastoreInformation), this, entityClass, daoConfig.useDataOwnerPartition, *viewQueries)

	protected suspend fun createQueries(datastoreInformation: IDatastoreInformation,viewQueryOnMain: String, viewQueryOnSecondary: Pair<String, String?>) =
		designDocumentProvider.createQueries(couchDbDispatcher.getClient(datastoreInformation), this, entityClass, viewQueryOnMain, viewQueryOnSecondary, daoConfig.useDataOwnerPartition)


	protected suspend fun <P> pagedViewQuery(datastoreInformation: IDatastoreInformation, viewName: String, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean, secondaryPartition: String? = null): ViewQuery =
		designDocumentProvider.pagedViewQuery(couchDbDispatcher.getClient(datastoreInformation), this, viewName, entityClass, startKey, endKey, pagination, descending, secondaryPartition)

	protected suspend fun <P> createPagedQueries(datastoreInformation: IDatastoreInformation, viewQueryOnMain: String, viewQueryOnSecondary: Pair<String, String?>, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean) =
		designDocumentProvider.createPagedQueries(couchDbDispatcher.getClient(datastoreInformation), this, entityClass, viewQueryOnMain, viewQueryOnSecondary, startKey, endKey, pagination, descending, daoConfig.useDataOwnerPartition)

	protected suspend fun <P> createPagedQueries(datastoreInformation: IDatastoreInformation, viewQueries: List<Pair<String, String?>>, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, descending: Boolean): ViewQueries =
		designDocumentProvider.createPagedQueries(couchDbDispatcher.getClient(datastoreInformation), this, entityClass, viewQueries, startKey, endKey, pagination, descending, daoConfig.useDataOwnerPartition)

	protected suspend fun <P> pagedViewQueryOfIds(datastoreInformation: IDatastoreInformation, viewName: String, startKey: P?, endKey: P?, pagination: PaginationOffset<P>, secondaryPartition: String? = null) =
		designDocumentProvider.pagedViewQueryOfIds(couchDbDispatcher.getClient(datastoreInformation), this, viewName, entityClass, startKey, endKey, pagination, secondaryPartition)

	override suspend fun getEntityWithFullQuorum(
		datastoreInformation: IDatastoreInformation,
		id: String,
		timeout: Duration?
	): T? {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val quorum = client.shards().shards.maxOf { it.value.size }

		return try {
				client.getWithQuorum(id, entityClass, quorum)?.let {
					cacheChain?.putInCache(datastoreInformation.getFullIdFor(id), it)
					postLoad(datastoreInformation, it)
				}
		} catch (_: DocumentNotFoundException) {
			null
		}
	}

	override suspend fun saveEntityWithFullQuorum(
		datastoreInformation: IDatastoreInformation,
		entity: T,
		timeout: Duration?
	): Pair<T, Boolean> {
		val client = couchDbDispatcher.getClient(datastoreInformation)
		val quorum = client.shards().shards.maxOf { it.value.size }

		try {
			return beforeSave(datastoreInformation, entity).let { e ->
				if (e.rev == null) {
					client.createWithQuorum(e, entityClass, quorum, timeout)
				} else {
					client.updateWithQuorum(e, entityClass, quorum, timeout)
				}.let {
					cacheChain?.putInCache(datastoreInformation.getFullIdFor(it.first.id), it.first)
					Pair(afterSave(datastoreInformation, it.first, e), it.second)
				}
			}
		} catch (e: CouchDbException) {
			cacheChain?.evictFromCache(datastoreInformation.getFullIdFor(entity.id))
			throw e
		}
	}
}
