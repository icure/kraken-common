/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.entities.embed.Identifier

interface RelatedPersonDAO : ConflictDAO<RelatedPerson> {
	fun findRelatedPersonsByIds(datastoreInformation: IDatastoreInformation, relatedPersonIds: Flow<String>): Flow<ViewQueryResultEvent>

	/**
	 * Retrieves the ids of all the [RelatedPerson]s with a delegation to [dataOwnerId] (legacy delegations or
	 * security metadata).
	 */
	fun listRelatedPersonIdsByDataOwner(datastoreInformation: IDatastoreInformation, dataOwnerId: String): Flow<String>

	/**
	 * Retrieves the ids of all the [RelatedPerson]s with a delegation to [dataOwnerId] where the concatenation of
	 * [RelatedPerson.lastName] and [RelatedPerson.firstName], sanitized, contains the sanitized [searchString].
	 */
	fun listRelatedPersonIdsByDataOwnerNameContainsFuzzy(datastoreInformation: IDatastoreInformation, searchString: String?, dataOwnerId: String, limit: Int? = null): Flow<String>

	/**
	 * Retrieves the ids of all the [RelatedPerson]s with a delegation to one of the [searchKeys] and at least one of
	 * the provided [identifiers] in [RelatedPerson.identifier].
	 */
	fun listRelatedPersonIdsByDataOwnerAndIdentifiers(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, identifiers: List<Identifier>): Flow<String>
}
