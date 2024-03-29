/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.TotalCount
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.HealthElementLogic
import org.taktik.icure.asynclogic.base.impl.EncryptableEntityLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import org.taktik.icure.validation.aspect.Fixer
import java.util.*

@Service
@Profile("app")
class HealthElementLogicImpl (
    private val filters: Filters,
    private val healthElementDAO: HealthElementDAO,
    sessionLogic: SessionInformationProvider,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    private val datastoreInstanceProvider: org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider,
    fixer: Fixer
) : EncryptableEntityLogic<HealthElement, HealthElementDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic), HealthElementLogic {

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

	override fun listHealthElementsByHCPartyIdAndSecretPatientKey(
		hcPartyId: String,
		secretPatientKey: String,
		offset: PaginationOffset<ComplexKey>
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			healthElementDAO
				.listHealthElementsByHCPartyIdAndSecretPatientKey(datastoreInformation, hcPartyId, secretPatientKey, offset.limitIncludingKey())
				.toPaginatedFlow<HealthElement>(offset.limit)
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

	override fun solveConflicts(limit: Int?): Flow<IdAndRev> =
		flow {
			val datastoreInformation = datastoreInstanceProvider.getInstanceAndGroup()

			emitAll(
				healthElementDAO.listConflicts(datastoreInformation).let { if (limit != null) it.take(limit) else it }.mapNotNull {
					healthElementDAO.get(datastoreInformation, it.id, Option.CONFLICTS)?.let { healthElement ->
						healthElement.conflicts?.mapNotNull { conflictingRevision -> healthElementDAO.get(datastoreInformation, healthElement.id, conflictingRevision) }
							?.fold(healthElement) { kept, conflict -> kept.merge(conflict).also { healthElementDAO.purge(datastoreInformation, conflict) } }
							?.let { mergedHealthElement -> healthElementDAO.save(datastoreInformation, mergedHealthElement) }
							?.let { savedHealthElement -> IdAndRev(savedHealthElement.id, savedHealthElement.rev) }
					}
				}
			)
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
