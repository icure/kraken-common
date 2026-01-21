/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.taktik.couchdb.TotalCount
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.HealthElementLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.validation.aspect.Fixer
import java.util.*

open class HealthElementLogicImpl(
	filters: Filters,
	private val healthElementDAO: HealthElementDAO,
	sessionLogic: SessionInformationProvider,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
) : EntityWithEncryptionMetadataLogic<HealthElement, HealthElementDAO>(
	fixer,
	sessionLogic,
	datastoreInstanceProvider,
	exchangeDataMapLogic,
	filters,
),
	HealthElementLogic {
	override fun entityWithUpdatedSecurityMetadata(
		entity: HealthElement,
		updatedMetadata: SecurityMetadata,
	): HealthElement = entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO(): HealthElementDAO = healthElementDAO

	override fun createEntities(entities: Flow<HealthElement>): Flow<HealthElement> = super.createEntities(
		entities
			.map { healthElement ->
				fix(healthElement, isCreate = true)
			},
	)

	override suspend fun getHealthElement(healthElementId: String): HealthElement? = getEntity(healthElementId)

	override fun getHealthElements(healthElementIds: Collection<String>): Flow<HealthElement> = getEntities(healthElementIds)

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listHealthElementIdsByDataOwnerPatientOpeningDate instead")
	override fun listHealthElementsByHcPartyAndSecretPatientKeys(
		hcPartyId: String,
		secretPatientKeys: List<String>,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			healthElementDAO.listHealthElementsByHCPartyAndSecretPatientKeys(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				secretPatientKeys,
			),
		)
	}

	override fun listHealthElementIdsByDataOwnerPatientOpeningDate(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			healthElementDAO.listHealthElementIdsByDataOwnerPatientOpeningDate(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending,
			),
		)
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listHealthElementIdsByDataOwnerPatientOpeningDate instead")
	override suspend fun listLatestHealthElementsByHcPartyAndSecretPatientKeys(
		hcPartyId: String,
		secretPatientKeys: List<String>,
	): List<HealthElement> {
		val datastoreInformation = getInstanceAndGroup()
		return healthElementDAO
			.listHealthElementsByHCPartyAndSecretPatientKeys(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				secretPatientKeys,
			).toList()
			.groupBy { it.healthElementId }
			.values
			.mapNotNull { value ->
				value.maxByOrNull { it: HealthElement ->
					it.modified ?: it.created ?: 0L
				}
			}
	}

	override suspend fun modifyHealthElement(healthElement: HealthElement) = fix(healthElement, isCreate = false) { fixedHealthElement ->
		modifyEntity(fixedHealthElement)
	}

	override fun modifyEntities(entities: Collection<HealthElement>): Flow<HealthElement> = flow {
		emitAll(super.modifyEntities(entities.map { fix(it, isCreate = false) }))
	}

	override suspend fun addDelegation(
		healthElementId: String,
		delegation: Delegation,
	): HealthElement? {
		val datastoreInformation = getInstanceAndGroup()
		val healthElement = getHealthElement(healthElementId)
		return delegation.delegatedTo?.let { healthcarePartyId ->
			healthElement?.let { c ->
				healthElementDAO.save(
					datastoreInformation,
					c.copy(
						delegations =
						c.delegations +
							mapOf(
								healthcarePartyId to setOf(delegation),
							),
					),
				)
			}
		} ?: healthElement
	}

	override suspend fun addDelegations(
		healthElementId: String,
		delegations: List<Delegation>,
	): HealthElement? {
		val datastoreInformation = getInstanceAndGroup()
		val healthElement = getHealthElement(healthElementId)
		return healthElement?.let {
			healthElementDAO.save(
				datastoreInformation,
				it.copy(
					delegations =
					it.delegations +
						delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } },
				),
			)
		}
	}

	override fun solveConflicts(
		limit: Int?,
		ids: List<String>?,
	) = flow {
		emitAll(
			doSolveConflicts(
				ids,
				limit,
				getInstanceAndGroup(),
			),
		)
	}

	protected fun doSolveConflicts(
		ids: List<String>?,
		limit: Int?,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		val flow =
			ids?.asFlow()?.mapNotNull { healthElementDAO.get(datastoreInformation, it, Option.CONFLICTS) }
				?: healthElementDAO
					.listConflicts(datastoreInformation)
					.mapNotNull { healthElementDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { healthElement ->
				healthElement.conflicts
					?.mapNotNull { conflictingRevision ->
						healthElementDAO.get(
							datastoreInformation,
							healthElement.id,
							conflictingRevision,
						)
					}?.fold(healthElement to emptyList<HealthElement>()) { (kept, toBePurged), conflict ->
						kept.merge(conflict) to toBePurged + conflict
					}?.let { (mergedHealthElement, toBePurged) ->
						healthElementDAO.save(datastoreInformation, mergedHealthElement).also {
							toBePurged.forEach {
								if (it.rev != null && it.rev != mergedHealthElement.rev) {
									healthElementDAO.purge(datastoreInformation, listOf(it)).single()
								}
							}
						}
					}
			}.collect { emit(IdAndRev(it.id, it.rev)) }
	}

	override fun filter(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<HealthElement>,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter, datastoreInformation).toSet(TreeSet())
		aggregateResults(
			ids = ids,
			limit = paginationOffset.limit,
			supplier = { healthElementIds: Collection<String> ->
				healthElementDAO.findHealthElementsByIds(datastoreInformation, healthElementIds.asFlow())
			},
			startDocumentId = paginationOffset.startDocumentId,
		).forEach { emit(it) }
		emit(TotalCount(ids.size))
	}
}
