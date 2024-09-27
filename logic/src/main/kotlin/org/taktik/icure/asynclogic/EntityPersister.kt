/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.domain.filter.AbstractFilter

interface EntityPersister<E : Identifiable<String>> {

	fun createEntities(entities: Collection<E>): Flow<E>

	fun modifyEntities(entities: Collection<E>): Flow<E>

	suspend fun deleteEntity(id: String, rev: String?): DocIdentifier
	fun deleteEntities(identifiers: Collection<IdAndRev>): Flow<DocIdentifier>
	suspend fun undeleteEntity(id: String, rev: String?): E
	fun undeleteEntities(identifiers: Collection<IdAndRev>): Flow<E>
	suspend fun purgeEntity(id: String, rev: String): DocIdentifier

	fun getEntities(identifiers: Collection<String>): Flow<E>
	fun getEntities(): Flow<E>
	fun getEntityIds(): Flow<String>

	suspend fun hasEntities(): Boolean

	suspend fun exists(id: String): Boolean

	suspend fun getEntity(id: String): E?

	fun getEntities(identifiers: Flow<String>): Flow<E>
	fun createEntities(entities: Flow<E>): Flow<E>
	fun modifyEntities(entities: Flow<E>): Flow<E>

	/**
	 * Retrieves the ids of the entities [E] matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter].
	 * @return a [Flow] of the ids matching the filter.
	 */
	fun matchEntitiesBy(filter: AbstractFilter<*>): Flow<String>
}
