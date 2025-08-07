/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.taktik.couchdb.Client
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.DesignDocument
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Revisionable
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.utils.ExternalFilterKey
import org.taktik.icure.exceptions.ConflictRequestException
import org.taktik.icure.exceptions.NotFoundRequestException
import java.time.Duration

// We also need those for compile-time constants in annotations.
const val DATA_OWNER_PARTITION = "DataOwner"
const val MAURICE_PARTITION = "Maurice"

enum class Partitions(val partitionName: String) {
	All(""),
	Main(""),
	DataOwner(DATA_OWNER_PARTITION),
	Maurice(MAURICE_PARTITION);

	companion object {

		fun valueOfOrNull(partition: String): Partitions? = runCatching {
			Partitions.valueOf(partition)
		}.getOrNull()

	}
}

interface GenericDAO<T : Identifiable<String>> : LookupDAO<T> {
	/**
	 * If true the DAO is a generic DAO for group-level entities.
	 * The views for these DAOs will always be automatically initialized when creating a new group and when updating the
	 * design document for existing groups.
	 * If false the views must be explicitly initialized on the groups that need it.
	 */
	val isGenericGroupDao get() = true
	val entityClass: Class<T>

	/**
	 * @throws io.icure.asyncjacksonhttpclient.exception.TimeoutException if the request takes longer than timeout
	 */
	suspend fun getEntityWithFullQuorum(
		datastoreInformation: IDatastoreInformation,
		id: String,
		timeout: Duration? = null,
	): T?

	/**
	 * @throws io.icure.asyncjacksonhttpclient.exception.TimeoutException if the request takes longer than timeout
	 */
	suspend fun saveEntityWithFullQuorum(
		datastoreInformation: IDatastoreInformation,
		entity: T,
		timeout: Duration? = null,
	): Pair<T, Boolean>

	/**
	 * Retrieves all the entities [T]s in a group in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param offset a [PaginationOffset] of [K] for pagination.
	 * @param keyClass the [Class] of the pagination offset key [K].
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [T]s.
	 */
	fun <K> getAllPaginated(datastoreInformation: IDatastoreInformation, offset: PaginationOffset<K>, keyClass: Class<K>): Flow<ViewQueryResultEvent>

	/**
	 * Creates a collection of new entities on the database. If there is a cache, then the created entities are also saved
	 * at all levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entities the [Collection] of entities to create.
	 * @return a [Flow] containing the created entities.
	 */
	fun <K : Collection<T>> create(datastoreInformation: IDatastoreInformation, entities: K): Flow<T>

	/**
	 * Saves many entities and returns detailed information on which entities could be saved successfully and which
	 * could not.
	 */
	fun saveBulk(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<BulkSaveResult<T>>

	/**
	 * Checks if the entity with the id passed as parameter exists on the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param id the id of the entity to check.
	 * @return true if the entity exists, false otherwise.
	 */
	suspend fun contains(datastoreInformation: IDatastoreInformation, id: String): Boolean

	/**
	 * Checks if there are any entities of the specified type on the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @return true if at least one entity is present, false otherwise.
	 */
	suspend fun hasAny(datastoreInformation: IDatastoreInformation): Boolean

	/**
	 * Gets all the entities of the defined type in the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @return a [Flow] containing the entities.
	 */
	fun getEntities(datastoreInformation: IDatastoreInformation): Flow<T>

	/**
	 * Gets the id of all the entities of the defined type in the database.
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param limit if not null, only the first nth results are returned.
	 * @return a [Flow] containing the ids of the entities.
	 */
	fun getEntityIds(datastoreInformation: IDatastoreInformation, limit: Int? = null): Flow<String>

	/**
	 * Retrieves a collection of entities from the database by their id. The returned flow will have the same order as
	 * the order of the ids passed as parameter. If an entity is not found, then it is ignored and no error will be
	 * returned. For each id, the cache is checked first if present. Also, if the cache is present, all the found
	 * entities will be stored in all the levels of the cache.
	 * @param datastoreInformation the datastore information to get the database client.
	 * @param ids a [Collection] containing the ids of the entities to retrieve.
	 * @return a [Flow] containing all the found entities.
	 */
	fun getEntities(datastoreInformation: IDatastoreInformation, ids: Collection<String>): Flow<T>

	/**
	 * Retrieves a collection of entities from the database by their id. The returned flow will have the same order as
	 * the order of the ids passed as parameter. If an entity is not found, then it is ignored and no error will be
	 * returned. For each id, the cache is checked first if present. Also, if the cache is present, all the found
	 * entities will be stored in all the levels of the cache.
	 *
	 * @param datastoreInformation the datastore information to get the database client.
	 * @param ids a [Flow] containing the ids of the entities to retrieve.
	 * @return a [Flow] containing all the found entities.
	 */
	fun getEntities(datastoreInformation: IDatastoreInformation, ids: Flow<String>): Flow<T>

	/**
	 * Soft-deletes a collection of entities by setting their deletionDate field and saving them to the database. If a
	 * cache is present, then the entities are removed from all the levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entities a [Collection] of entities to soft-delete.
	 * @return a [Flow] of [DocIdentifier] related to the deleted entities.
	 */
	fun remove(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<BulkSaveResult<T>>

	/**
	 * Hard-deletes an entity. If a cache is present, then the entity is removed from all the levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity to delete.
	 * @return a [DocIdentifier] related to the deleted entity.
	 */
	fun purge(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<BulkSaveResult<DocIdentifier>>

	/**
	 * Reverts the soft-deleting of a collection of entities by setting their deletionDate field to null and saving
	 * them to the database. If a cache is present, then the entities are also removed form all the levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entities a [Collection] of entities to undelete.
	 * @return a [Flow] of [DocIdentifier] related to the undeleted entities.
	 */
	fun unRemove(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<BulkSaveResult<T>>

	/**
	 * Creates or updates the view design documents for this entity type.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param updateIfExists updates the design docs if already existing
	 * @param dryRun if true, it will retrieve the design docs to update, but it will not actually perform the update.
	 * @param partition if not [Partitions.All], only the documents on that partition will be generated.
	 * @param ignoreIfUnchanged if true, it will not generate all the design docs that are unchanged w.r.t. the existing ones.
	 * @return a [List] containing the updated [DesignDocument]s.
	 */
	suspend fun forceInitStandardDesignDocument(
		datastoreInformation: IDatastoreInformation,
		updateIfExists: Boolean = true,
		dryRun: Boolean = false,
		partition: Partitions = Partitions.All,
		ignoreIfUnchanged: Boolean = false
	): List<DesignDocument>

	/**
	 * Creates or updates the view design documents for this entity type.
	 *
	 * @param client the database [Client].
	 * @param updateIfExists updates the design docs if already existing
	 * @param dryRun if true, it will retrieve the design docs to update, but it will not actually perform the update.
	 * @param partition if not [Partitions.All], only the documents on that partition will be generated.
	 * @param ignoreIfUnchanged if true, it will not generate all the design docs that are unchanged w.r.t. the existing ones.
	 * @return a [List] containing the updated [DesignDocument]s.
	 */
	suspend fun forceInitStandardDesignDocument(
		client: Client,
		updateIfExists: Boolean = true,
		dryRun: Boolean = false,
		partition: Partitions = Partitions.All,
		ignoreIfUnchanged: Boolean = false
	): List<DesignDocument>

	/**
	 * Creates System design documents for this entity type.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 */
	suspend fun initSystemDocumentIfAbsent(datastoreInformation: IDatastoreInformation)

	/**
	 * Creates System design documents for this entity type.
	 *
	 * @param client the database client.
	 */
	suspend fun initSystemDocumentIfAbsent(client: Client)

	/**
	 * Makes a simple query to a view in the specified [partition] to ensure that the indexation starts for that
	 * design document.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param partition the [Partitions] to index.
	 */
	suspend fun warmupPartition(datastoreInformation: IDatastoreInformation, partition: Partitions)

	/**
	 * Makes a simple query to all the specified [designDocuments] to ensure tha the indexation starts for their partition.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param designDocuments a [List] of [DesignDocument] to index.
	 */
	suspend fun warmupExternalDesignDocs(datastoreInformation: IDatastoreInformation, designDocuments: List<DesignDocument>)

	/**
	 * Creates or updates the view design documents for this entity type from one or more external sources.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param partitionsWithRepo a [Map] that associates a partition name to the url of the repository where the view
	 * for that partition are located.
	 * @param updateIfExists updates the design docs if already existing
	 * @param dryRun if true, it will retrieve the design docs to update, but it will not actually perform the update.
	 * @param ignoreIfUnchanged if true, it will not generate all the design docs that are unchanged w.r.t. the existing ones.
	 * @return a [List] containing the updated [DesignDocument]s.
	 */
	suspend fun forceInitExternalDesignDocument(
		datastoreInformation: IDatastoreInformation,
		partitionsWithRepo: Map<String, String>,
		updateIfExists: Boolean,
		dryRun: Boolean,
		ignoreIfUnchanged: Boolean = false
	): List<DesignDocument>

	/**
	 * Retrieves all the entities id for a custom view.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param viewName the name of the view to query.
	 * @param partitionName the secondary partition where the view is located.
	 * @param startKey an optional start key for the query.
	 * @param endKey an optional end key for the query.
	 * @return a [Flow] containing the ids of the entities between [startKey] (if provided) and [endKey] (if provided).
	 */
	fun listEntitiesIdInCustomView(
		datastoreInformation: IDatastoreInformation,
		viewName: String,
		partitionName: String,
		startKey: ExternalFilterKey?,
		endKey: ExternalFilterKey?
	): Flow<String>
}

suspend fun <T> GenericDAO<T>.getEntitiesWithExpectedRev(
	datastoreInformation: IDatastoreInformation,
	identifiers: Collection<IdAndRev>
): List<T> where T : Revisionable<String> {
	val expectedRevById = identifiers.associate { it.id to it.rev }
	return getEntities(
		datastoreInformation,
		identifiers.map { it.id }
	).toList().filter {
		expectedRevById.getValue(it.id).let { expectedRev -> expectedRev == null || expectedRev == it.rev }
	}
}

suspend fun <T> GenericDAO<T>.getEntityWithExpectedRev(
	datastoreInformation: IDatastoreInformation,
	id: String,
	rev: String?
): T where T : Revisionable<String> {
	val retrieved = get(datastoreInformation, id)
		?: throw NotFoundRequestException("Entity with id $id not found")
	if (rev != null && retrieved.rev != rev) {
		throw ConflictRequestException("Revision does not match for entity with id $id")
	}
	return retrieved
}

