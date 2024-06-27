/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.TotalCount
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.HealthElementLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import org.taktik.icure.validation.aspect.Fixer
import java.util.TreeSet

open class HealthElementLogicImpl (
    private val filters: Filters,
    private val healthElementDAO: HealthElementDAO,
    sessionLogic: SessionInformationProvider,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    private val datastoreInstanceProvider: org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider,
    fixer: Fixer
) : EntityWithEncryptionMetadataLogic<HealthElement, HealthElementDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic), HealthElementLogic {

	override fun entityWithUpdatedSecurityMetadata(
		entity: HealthElement,
		updatedMetadata: SecurityMetadata
	): HealthElement = entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO(): HealthElementDAO {
		return healthElementDAO
	}

	override fun createEntities(entities: Flow<HealthElement>): Flow<HealthElement> =
		super.createEntities(
			entities
				.map { healthElement ->
					fix(healthElement).also { fixedHealElement ->
						if (fixedHealElement.rev != null) {
							throw IllegalArgumentException("A new entity should not have a rev")
						}
					}
				}
		)

	override suspend fun getHealthElement(healthElementId: String): HealthElement? =
		getEntity(healthElementId)

	override fun getHealthElements(healthElementIds: Collection<String>): Flow<HealthElement> =
		getEntities(healthElementIds)

	override fun listHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>) =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthElementDAO.listHealthElementsByHCPartyAndSecretPatientKeys(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys))
		}

	override fun listHealthElementIdsByDataOwnerPatientOpeningDate(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			healthElementDAO.listHealthElementIdsByDataOwnerPatientOpeningDate(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending
			)
		)
	}

	override fun listHealthElementIdsByHcParty(hcpId: String) =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(
				mergeUniqueIdsForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcpId)) { key ->
					healthElementDAO.listHealthElementsByHcParty(datastoreInformation, key)
				}
			)
		}


	override fun listHealthElementIdsByHcPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>) =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthElementDAO.listHealthElementIdsByHcPartyAndSecretPatientKeys(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys))
		}


	override suspend fun listLatestHealthElementsByHcPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): List<HealthElement> {
		val datastoreInformation = getInstanceAndGroup()
		return healthElementDAO.listHealthElementsByHCPartyAndSecretPatientKeys(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys).toList()
			.groupBy { it.healthElementId }.values.mapNotNull { value ->
				value.maxByOrNull { it: HealthElement ->
					it.modified ?: it.created ?: 0L
				}
			}
	}

	override fun listHealthElementIdsByHcPartyAndCodes(hcPartyId: String, codeType: String, codeNumber: String) =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthElementDAO.listHealthElementsByHCPartyAndCodes(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), codeType, codeNumber))
		}


	override fun listHealthElementIdsByHcPartyAndTags(hcPartyId: String, tagType: String, tagCode: String) =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthElementDAO.listHealthElementsByHCPartyAndTags(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), tagType, tagCode))
		}


	override fun listHealthElementsIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>) =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthElementDAO.listHealthElementsIdsByHcPartyAndIdentifiers(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), identifiers))
		}


	override fun listHealthElementIdsByHcPartyAndStatus(hcPartyId: String, status: Int) =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(healthElementDAO.listHealthElementsByHCPartyAndStatus(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), status))
		}


	override fun deleteHealthElements(ids: Set<String>): Flow<DocIdentifier> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(
					healthElementDAO.remove(datastoreInformation, getHealthElements(ids.toList()).toList())
			)
		}
	override suspend fun modifyHealthElement(healthElement: HealthElement): HealthElement? =
			fix(healthElement) { fixedHealthElement ->
					modifyEntities(setOf(fixedHealthElement)).firstOrNull()
			}

	override fun modifyEntities(entities: Collection<HealthElement>): Flow<HealthElement> = flow {
		emitAll(super.modifyEntities(entities.map { fix(it) }))
	}


	override suspend fun addDelegation(healthElementId: String, delegation: Delegation): HealthElement? {
		val datastoreInformation = getInstanceAndGroup()
		val healthElement = getHealthElement(healthElementId)
		return delegation.delegatedTo?.let { healthcarePartyId ->
			healthElement?.let { c ->
				healthElementDAO.save(
					datastoreInformation,
					c.copy(
						delegations = c.delegations + mapOf(
							healthcarePartyId to setOf(delegation)
						)
					)
				)
			}
		} ?: healthElement
	}

	override suspend fun addDelegations(healthElementId: String, delegations: List<Delegation>): HealthElement? {
		val datastoreInformation = getInstanceAndGroup()
		val healthElement = getHealthElement(healthElementId)
		return healthElement?.let {
			healthElementDAO.save(
				datastoreInformation,
				it.copy(
					delegations = it.delegations +
						delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } }
				)
			)
		}
	}

	override fun solveConflicts(limit: Int?, ids: List<String>?) = flow { emitAll(doSolveConflicts(
		ids,
		limit,
		getInstanceAndGroup()
	)) }

	protected fun doSolveConflicts(
		ids: List<String>?,
		limit: Int?,
		datastoreInformation: IDatastoreInformation,
	) =  flow {
		val flow = ids?.asFlow()?.mapNotNull { healthElementDAO.get(datastoreInformation, it, Option.CONFLICTS) }
			?: healthElementDAO.listConflicts(datastoreInformation)
				.mapNotNull { healthElementDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { healthElement ->
				healthElement.conflicts?.mapNotNull { conflictingRevision ->
					healthElementDAO.get(
						datastoreInformation, healthElement.id, conflictingRevision
					)
				}?.fold(healthElement to emptyList<HealthElement>()) { (kept, toBePurged), conflict ->
					kept.merge(conflict) to toBePurged + conflict
				}?.let { (mergedHealthElement, toBePurged) ->
					healthElementDAO.save(datastoreInformation, mergedHealthElement).also {
						toBePurged.forEach {
							healthElementDAO.purge(datastoreInformation, it)
						}
					}
				}
			}
			.collect { emit(IdAndRev(it.id, it.rev)) }
	}

	override fun filter(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<HealthElement>) =
			flow {
				val datastoreInformation = getInstanceAndGroup()
				val ids = filters.resolve(filter.filter).toSet(TreeSet())
				aggregateResults(
					ids = ids,
					limit = paginationOffset.limit,
					supplier = { healthElementIds: Collection<String> -> healthElementDAO.findHealthElementsByIds(datastoreInformation, healthElementIds.asFlow()) },
					startDocumentId = paginationOffset.startDocumentId
				).forEach { emit(it) }
				emit(TotalCount(ids.size))
			}

}
