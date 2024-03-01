/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Place
import org.taktik.icure.pagination.PaginationElement

interface PlaceLogic : EntityPersister<Place, String> {
	suspend fun createPlace(place: Place): Place?
	fun deletePlace(ids: List<String>): Flow<DocIdentifier>

	suspend fun getPlace(place: String): Place?
	suspend fun modifyPlace(place: Place): Place?

	/**
	 * Retrieves all the [Place]s in a group in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Place]s.
	 */
	fun getAllPlaces(paginationOffset: PaginationOffset<Nothing>): Flow<PaginationElement>
}
