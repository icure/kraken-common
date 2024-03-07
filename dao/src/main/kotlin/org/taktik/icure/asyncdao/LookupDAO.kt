/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import org.taktik.couchdb.entity.Option
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation

interface LookupDAO<T : Identifiable<String>> {
	/**
	 * Get an existing entity
	 *
	 * @param datastoreInformation an IDataStoreInformation instance
	 * @param id Id of the entity to get
	 * @param options Any eventual option for fetching the entity. Used if you need to retrieve conflicting revisions,
	 * revisions' history, etc...
	 * @return The entity
	 */
	suspend fun get(datastoreInformation: IDatastoreInformation, id: String, vararg options: Option): T?

	/**
	 * Retrieves an entity from the database by its id. If the cache is not null, it will access the cache first.
	 * If the element is not found in the cache, but found in the database and a cache is present, then the element is
	 * stored at all the levels of the cache.
	 *
	 * @param datastoreInformation the datastore information to get the database client.
	 * @param id the id of the entity to retrieve.
	 * @param rev the rev of the entity to retrieve.
	 * @param options 0 or more [Option] to pass to the database client.
	 * @return the entity or null if not found.
	 */
	suspend fun get(datastoreInformation: IDatastoreInformation, id: String, rev: String?, vararg options: Option): T?

	/**
	 * Creates a new entity on the database. If there is a cache, then the created entity is also saved at all the
	 * levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity to create.
	 * @return the created entity or null.
	 */
	suspend fun create(datastoreInformation: IDatastoreInformation, entity: T): T?

	/**
	 * Updates an existing entity on the database. If the entity rev field is null, the entity will be created. If
	 * there is a cache, then the created entity is also saved at all the levels of the cache.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to get the database client.
	 * @param entity the entity to create.
	 * @return the updated entity or null.
	 */
	suspend fun save(datastoreInformation: IDatastoreInformation, entity: T): T?
}
