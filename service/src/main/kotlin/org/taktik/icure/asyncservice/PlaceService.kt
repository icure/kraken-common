/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.entities.Place

interface PlaceService {
	fun getAllPlaces(): Flow<Place>
	suspend fun createPlace(place: Place): Place?
	fun deletePlace(ids: List<String>): Flow<DocIdentifier>
	suspend fun getPlace(place: String): Place?
	suspend fun modifyPlace(place: Place): Place?
	fun deletePlaces(identifiers: Set<String>): Flow<DocIdentifier>
}
