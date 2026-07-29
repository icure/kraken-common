/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.taktik.couchdb.TotalCount
import org.taktik.icure.asyncdao.RelatedPersonDAO
import org.taktik.icure.asynclogic.ConflictResolutionLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.RelatedPersonLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.RelatedPerson
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.mergers.Merger
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import org.taktik.icure.validation.aspect.Fixer
import java.util.TreeSet

open class RelatedPersonLogicImpl(
	filters: Filters,
	private val relatedPersonDAO: RelatedPersonDAO,
	sessionLogic: SessionInformationProvider,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	relatedPersonMerger: Merger<RelatedPerson>,
) : EntityWithEncryptionMetadataLogic<RelatedPerson, RelatedPersonDAO>(
	fixer,
	sessionLogic,
	datastoreInstanceProvider,
	exchangeDataMapLogic,
	filters,
),
	ConflictResolutionLogic<RelatedPerson> by ConflictResolutionLogicImpl(relatedPersonDAO, relatedPersonMerger, datastoreInstanceProvider),
	RelatedPersonLogic {
	override fun entityWithUpdatedSecurityMetadata(
		entity: RelatedPerson,
		updatedMetadata: SecurityMetadata,
	): RelatedPerson = entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO(): RelatedPersonDAO = relatedPersonDAO

	override fun createEntities(entities: Flow<RelatedPerson>): Flow<RelatedPerson> = super.createEntities(
		entities.map { relatedPerson ->
			fix(relatedPerson, isCreate = true)
		},
	)

	override suspend fun getRelatedPerson(relatedPersonId: String): RelatedPerson? = getEntity(relatedPersonId)

	override fun getRelatedPersons(relatedPersonIds: Collection<String>): Flow<RelatedPerson> = getEntities(relatedPersonIds)

	override fun listRelatedPersonIdsByDataOwner(dataOwnerId: String): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			mergeUniqueIdsForSearchKeys(getAllSearchKeysIfCurrentDataOwner(dataOwnerId)) { key ->
				relatedPersonDAO.listRelatedPersonIdsByDataOwner(datastoreInformation, key)
			},
		)
	}

	override fun filter(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<RelatedPerson>,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter, datastoreInformation).toSet(TreeSet())
		aggregateResults(
			ids = ids,
			limit = paginationOffset.limit,
			supplier = { relatedPersonIds: Collection<String> ->
				relatedPersonDAO.findRelatedPersonsByIds(datastoreInformation, relatedPersonIds.asFlow())
			},
			startDocumentId = paginationOffset.startDocumentId,
		).forEach { emit(it) }
		emit(TotalCount(ids.size))
	}
}
