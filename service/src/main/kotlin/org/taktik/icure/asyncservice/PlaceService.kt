/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Place
import org.taktik.icure.pagination.PaginationElement

interface PlaceService {

	/**
	 * Retrieves all the [Place]s in a group in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Place]s.
	 * @throws AccessDeniedException if the current user is not an admin or an healthcare party.
	 */
	fun getAllPlaces(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement>

	/**
	 * Retrieves all the [Place]s in a group.
	 *
	 * @return a [Flow] of [PaginationElement] containing the [Place]s.
	 * @throws AccessDeniedException if the current user is not an admin or an healthcare party.
	 */
	fun getAllPlaces(): Flow<Place>
	suspend fun createPlace(place: Place): Place?
	suspend fun deletePlace(id: String, rev: String?): DocIdentifier
	suspend fun getPlace(place: String): Place?
	suspend fun modifyPlace(place: Place): Place?
	fun deletePlaces(identifiers: List<IdAndRev>): Flow<DocIdentifier>
}
