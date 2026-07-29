/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.RelatedPerson

interface RelatedPersonLogic :
	EntityPersister<RelatedPerson>,
	EntityWithSecureDelegationsLogic<RelatedPerson>,
	ConflictResolutionLogic<RelatedPerson> {
	suspend fun getRelatedPerson(relatedPersonId: String): RelatedPerson?
	fun getRelatedPersons(relatedPersonIds: Collection<String>): Flow<RelatedPerson>

	/**
	 * Retrieves the ids of all the [RelatedPerson]s with a delegation to [dataOwnerId] (plus all the current access
	 * keys if that is equal to the data owner id of the user making the request).
	 *
	 * @param dataOwnerId the id of the data owner.
	 * @return a [Flow] of RelatedPerson ids.
	 */
	fun listRelatedPersonIdsByDataOwner(dataOwnerId: String): Flow<String>

	fun filter(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<RelatedPerson>,
	): Flow<ViewQueryResultEvent>
}
