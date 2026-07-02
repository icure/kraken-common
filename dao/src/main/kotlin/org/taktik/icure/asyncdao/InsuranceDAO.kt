/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Insurance
import org.taktik.icure.entities.embed.Identifier

interface InsuranceDAO : ConflictDAO<Insurance> {
	fun listInsurancesByCode(datastoreInformation: IDatastoreInformation, code: String): Flow<Insurance>

	fun listInsurancesByName(datastoreInformation: IDatastoreInformation, name: String): Flow<Insurance>

	/**
	 * Retrieves the ids of all the [Insurance]s that have at least one of the provided [identifiers] in
	 * [Insurance.identifier].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to specify group and CouchDB instance.
	 * @param identifiers the [Identifier]s to search for.
	 * @return a [Flow] of the ids of the matching [Insurance]s.
	 */
	fun listInsuranceIdsByIdentifiers(datastoreInformation: IDatastoreInformation, identifiers: List<Identifier>): Flow<String>

	/**
	 * Retrieves the ids of all the [Insurance]s that have a code stub in [Insurance.codes] with the provided
	 * [codeType] and, optionally, [codeCode].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to specify group and CouchDB instance.
	 * @param codeType the type of the code to filter on.
	 * @param codeCode the code value to match, or null to match any code of the provided [codeType].
	 * @return a [Flow] of the ids of the matching [Insurance]s.
	 */
	fun listInsuranceIdsByCode(datastoreInformation: IDatastoreInformation, codeType: String, codeCode: String?): Flow<String>

	/**
	 * Retrieves the ids of all the [Insurance]s that have a code stub in [Insurance.tags] with the provided
	 * [tagType] and, optionally, [tagCode].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to specify group and CouchDB instance.
	 * @param tagType the type of the tag to filter on.
	 * @param tagCode the tag code value to match, or null to match any tag of the provided [tagType].
	 * @return a [Flow] of the ids of the matching [Insurance]s.
	 */
	fun listInsuranceIdsByTag(datastoreInformation: IDatastoreInformation, tagType: String, tagCode: String?): Flow<String>

	/**
	 * Retrieves all the insurances in the group specified in the [IDatastoreInformation] in a format
	 * for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to specify group and CouchDB instance.
	 * @param paginationOffset a [PaginationOffset] of [Nothing] (i.e. with an always-null start key) for the pagination.
	 * @return a [Flow] of [Insurance]s wrapped in [ViewQueryResultEvent]s for pagination.
	 */
	fun getAllInsurances(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<Nothing>): Flow<ViewQueryResultEvent>
}
