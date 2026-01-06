/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.taktik.icure.asyncdao.PlaceDAO
import org.taktik.icure.asynclogic.PlaceLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Place
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

open class PlaceLogicImpl(
	private val placeDAO: PlaceDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : GenericLogicImpl<Place, PlaceDAO>(fixer, datastoreInstanceProvider, filters),
	PlaceLogic {
	override suspend fun createPlace(place: Place) = fix(place, isCreate = true) { fixedPlace ->
		if (fixedPlace.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		val datastoreInformation = getInstanceAndGroup()
		placeDAO.create(datastoreInformation, fixedPlace)
	}

	override suspend fun getPlace(place: String): Place? {
		val datastoreInformation = getInstanceAndGroup()
		return placeDAO.get(datastoreInformation, place)
	}

	override suspend fun modifyPlace(place: Place): Place = fix(place, isCreate = false) { fixedPlace ->
		val datastoreInformation = getInstanceAndGroup()
		placeDAO.save(datastoreInformation, fixedPlace)
	}

	override fun getAllPlaces(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			placeDAO
				.getAllPaginated(datastoreInformation, paginationOffset.limitIncludingKey(), Nothing::class.java)
				.toPaginatedFlow<Place>(paginationOffset.limit),
		)
	}

	override fun getGenericDAO(): PlaceDAO = placeDAO
}
