/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.asyncdao.PlaceDAO
import org.taktik.icure.asynclogic.PlaceLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Place
import org.taktik.icure.exceptions.DeletionException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class PlaceLogicImpl(
    private val placeDAO: PlaceDAO,
    datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer
) : GenericLogicImpl<Place, PlaceDAO>(fixer, datastoreInstanceProvider), PlaceLogic {

	override suspend fun createPlace(place: Place): Place? = fix(place) { fixedPlace ->
		if(fixedPlace.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		val datastoreInformation = getInstanceAndGroup()
		placeDAO.create(datastoreInformation, fixedPlace)
	}

	override fun deletePlace(ids: List<String>): Flow<DocIdentifier> = flow {
		try {
			emitAll(deleteEntities(ids))
		} catch (e: Exception) {
			throw DeletionException(e.message, e)
		}
	}

	override suspend fun getPlace(place: String): Place? {
		val datastoreInformation = getInstanceAndGroup()
		return placeDAO.get(datastoreInformation, place)
	}

	override suspend fun modifyPlace(place: Place): Place? = fix(place) { fixedPlace ->
		val datastoreInformation = getInstanceAndGroup()
		placeDAO.save(datastoreInformation, fixedPlace)
	}

	override fun getAllPlaces(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(placeDAO
			.getAllPaginated(datastoreInformation, paginationOffset.limitIncludingKey(), Nothing::class.java)
			.toPaginatedFlow<Place>(paginationOffset.limit)
		)
	}

	override fun getGenericDAO(): PlaceDAO {
		return placeDAO
	}
}
