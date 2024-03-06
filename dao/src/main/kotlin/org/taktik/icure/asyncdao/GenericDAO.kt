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

	fun <K : Collection<T>> create(datastoreInformation: IDatastoreInformation, entities: K): Flow<T>

	/**
	 * @deprecated consider using [saveBulk]
	 */
	fun <K : Collection<T>> save(datastoreInformation: IDatastoreInformation, entities: K): Flow<T>

	/**
	 * Saves many entities and returns detailed information on which entities could be saved successfully and which
	 * could not.
	 */
	fun saveBulk(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<BulkSaveResult<T>>

	suspend fun contains(datastoreInformation: IDatastoreInformation, id: String): Boolean
	suspend fun hasAny(datastoreInformation: IDatastoreInformation): Boolean

	fun getEntities(datastoreInformation: IDatastoreInformation): Flow<T>
	fun getEntityIds(datastoreInformation: IDatastoreInformation, limit: Int? = null): Flow<String>

	fun getEntities(datastoreInformation: IDatastoreInformation, ids: Collection<String>): Flow<T>
	fun getEntities(datastoreInformation: IDatastoreInformation, ids: Flow<String>): Flow<T>

	suspend fun remove(datastoreInformation: IDatastoreInformation, entity: T): DocIdentifier
	fun remove(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<DocIdentifier>
	suspend fun purge(datastoreInformation: IDatastoreInformation, entity: T): DocIdentifier
	fun unRemove(datastoreInformation: IDatastoreInformation, entities: Collection<T>): Flow<DocIdentifier>
	suspend fun unRemove(datastoreInformation: IDatastoreInformation, entity: T): DocIdentifier
	suspend fun forceInitStandardDesignDocument(datastoreInformation: IDatastoreInformation, updateIfExists: Boolean = true, dryRun: Boolean = false): List<DesignDocument>
	suspend fun forceInitStandardDesignDocument(client: Client, updateIfExists: Boolean = true, dryRun: Boolean = false): List<DesignDocument>
	suspend fun initSystemDocumentIfAbsent(datastoreInformation: IDatastoreInformation)
	suspend fun initSystemDocumentIfAbsent(client: Client)
}
