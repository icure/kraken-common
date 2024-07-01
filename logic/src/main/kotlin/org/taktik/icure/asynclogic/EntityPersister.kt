/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.entities.utils.ExternalFilterKey

interface EntityPersister<E, I> {

	fun createEntities(entities: Collection<E>): Flow<E>

	fun modifyEntities(entities: Collection<E>): Flow<E>

	fun deleteEntities(identifiers: Collection<I>): Flow<DocIdentifier>
	fun undeleteByIds(identifiers: Collection<I>): Flow<DocIdentifier>

	fun getEntities(identifiers: Collection<I>): Flow<E>
	fun getEntities(): Flow<E>
	fun getEntityIds(): Flow<I>
	fun listEntityIdsInCustomView(viewName: String, partitionName: String, startKey: ExternalFilterKey<*>?, endKey: ExternalFilterKey<*>?): Flow<I>

	suspend fun hasEntities(): Boolean

	suspend fun exists(id: I): Boolean

	suspend fun getEntity(id: I): E?

	fun getEntities(identifiers: Flow<I>): Flow<E>
	fun createEntities(entities: Flow<E>): Flow<E>
	fun modifyEntities(entities: Flow<E>): Flow<E>
	fun deleteEntities(identifiers: Flow<I>): Flow<DocIdentifier>

}
