/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.Client
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.DesignDocument
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.asyncdao.results.BulkSaveResult
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset

const val DATA_OWNER_PARTITION = "DataOwner"

const val MAURICE_PARTITION = "Maurice"

interface GenericDAO<T : Identifiable<String>> : LookupDAO<T> {
	/**
	 * If true the DAO is for group-level entities, if false the DAO is for global entities.
	 */
	val isGroupDao get() = true

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
	 * @deprecated consider using [saveBulk]
	 * Creates or updates a collection of new entities on the database. If there is a cache, then the created entities
	 * are also saved at all levels of the cache. If an entity already exists, but it has a wrong rev, than it will not
	 * be created or updated and will be ignored in the final result.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entities the [Collection] of entities to create.
	 * @return a [Flow] containing the created entities.
	 */
	fun <K : Collection<T>> save(datastoreInformation: IDatastoreInformation, entities: K): Flow<T>

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
	 * Soft-deletes an entity by setting its deletionDate field and saving it to the database. If a cache is present,
	 * then the entity is removed from all the levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity to delete.
	 * @return a [DocIdentifier] related to the deleted entity.
	 */
	suspend fun remove(datastoreInformation: IDatastoreInformation, entity: T): DocIdentifier

	/**
	 * Soft-deletes a collection of entities by setting their deletionDate field and saving them to the database. If a
	 * cache is present, then the entities are removed from all the levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entities a [Collection] of entities to soft-delete.
	 * @return a [Flow] of [DocIdentifier] related to the deleted entities.
	 */
	fun remove(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<DocIdentifier>

	/**
	 * Hard-deletes an entity. If a cache is present, then the entity is removed from all the levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity to delete.
	 * @return a [DocIdentifier] related to the deleted entity.
	 */
	suspend fun purge(datastoreInformation: IDatastoreInformation, entity: T): DocIdentifier

	/**
	 * Reverts the soft-deleting of a collection of entities by setting their deletionDate field to null and saving
	 * them to the database. If a cache is present, then the entities are also removed form all the levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entities a [Collection] of entities to undelete.
	 * @return a [Flow] of [DocIdentifier] related to the undeleted entities.
	 */
	fun unRemove(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<DocIdentifier>

	/**
	 * Reverts the soft-deleting of an entity by setting its deletionDate field to null and saving it to the database.
	 * If a cache is present, then the entity is also removed form all the level of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity to undelete.
	 * @return a [DocIdentifier] related to the undeleted entity.
	 */
	suspend fun unRemove(datastoreInformation: IDatastoreInformation, entity: T): DocIdentifier

	/**
	 * Creates the view design documents for this entity type.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param updateIfExists updates the design docs if already existing
	 */
	suspend fun forceInitStandardDesignDocument(datastoreInformation: IDatastoreInformation, updateIfExists: Boolean = true, dryRun: Boolean = false): List<DesignDocument>

	/**
	 * Creates the view design documents for this entity type.
	 *
	 * @param client the database client.
	 * @param updateIfExists updates the design docs if already existing
	 */
	suspend fun forceInitStandardDesignDocument(client: Client, updateIfExists: Boolean = true, dryRun: Boolean = false): List<DesignDocument>

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
}
