/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.asyncdao.GenericDAO
import org.taktik.icure.asyncdao.results.filterSuccessfulUpdates
import org.taktik.icure.asynclogic.EntityPersister
import org.taktik.icure.asynclogic.base.AutoFixableLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.validation.aspect.Fixer

abstract class GenericLogicImpl<E : Identifiable<String>, D : GenericDAO<E>>(
	fixer: Fixer,
	private val datastoreInstanceProvider: DatastoreInstanceProvider,
	protected val filters: Filters
) : AutoFixableLogic<E>(fixer), EntityPersister<E, String> {

	protected open suspend fun getInstanceAndGroup(): IDatastoreInformation = datastoreInstanceProvider.getInstanceAndGroup()

	override fun createEntities(entities: Collection<E>): Flow<E> = flow {
		emitAll(getGenericDAO().create(getInstanceAndGroup(), entities.map { fix(it) }))
	}

	override fun modifyEntities(entities: Collection<E>): Flow<E> = flow {
		emitAll(getGenericDAO()
			.saveBulk(
				datastoreInformation = getInstanceAndGroup(),
				entities = entities.map { fix(it) }
			).filterSuccessfulUpdates()
		)
	}

	override fun deleteEntities(identifiers: Collection<String>): Flow<DocIdentifier> = flow {
		val entities = getGenericDAO().getEntities(getInstanceAndGroup(), identifiers).toList()
		emitAll(getGenericDAO().remove(getInstanceAndGroup(), entities))
	}

	override fun undeleteByIds(identifiers: Collection<String>): Flow<DocIdentifier> = flow {
		val entities = getGenericDAO().getEntities(getInstanceAndGroup(), identifiers).toList()
		emitAll(getGenericDAO().unRemove(getInstanceAndGroup(), entities))
	}

	override fun getEntities(identifiers: Collection<String>): Flow<E> = flow {
		emitAll(getGenericDAO().getEntities(getInstanceAndGroup(), identifiers))
	}

	override fun getEntities(): Flow<E> = flow {
		emitAll(getGenericDAO().getEntities(getInstanceAndGroup()))
	}

	override fun getEntityIds(): Flow<String> = flow {
		emitAll(getGenericDAO().getEntityIds(getInstanceAndGroup()))
	}

	override suspend fun hasEntities(): Boolean {
		return getGenericDAO().hasAny(getInstanceAndGroup())
	}

	override suspend fun exists(id: String): Boolean {
		return getGenericDAO().contains(getInstanceAndGroup(), id)
	}

	override suspend fun getEntity(id: String): E? {
		return getGenericDAO().get(getInstanceAndGroup(), id)
	}

	override fun getEntities(identifiers: Flow<String>): Flow<E> = flow {
		emitAll(getGenericDAO().getEntities(getInstanceAndGroup(), identifiers))
	}

	override fun createEntities(entities: Flow<E>): Flow<E> = flow {
		emitAll(this@GenericLogicImpl.createEntities(entities.toList()))

	}

	override fun modifyEntities(entities: Flow<E>): Flow<E> = flow {
		emitAll(this@GenericLogicImpl.modifyEntities(entities.toList()))
	}

	override fun deleteEntities(identifiers: Flow<String>): Flow<DocIdentifier> = flow{
		val entities = getGenericDAO().getEntities(getInstanceAndGroup(), identifiers).toList()
		emitAll(getGenericDAO().remove(getInstanceAndGroup(), entities))
	}

	override fun matchEntitiesBy(filter: AbstractFilter<*>): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(filters.resolve(filter, datastoreInformation))
	}

	protected abstract fun getGenericDAO(): D

	fun getEntityClass(): Class<E> = getGenericDAO().entityClass
}
