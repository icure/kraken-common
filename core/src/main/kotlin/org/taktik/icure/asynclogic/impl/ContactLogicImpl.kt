/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.couchdb.exception.UpdateConflictException
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.base.impl.EncryptableEntityLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.data.LabelledOccurence
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.pimpWithContactInformation
import org.taktik.icure.exceptions.BulkUpdateConflictException
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import org.taktik.icure.validation.aspect.Fixer
import java.util.*

@Service
@Profile("app")
class ContactLogicImpl(
    private val contactDAO: ContactDAO,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    private val sessionLogic: SessionInformationProvider,
    private val datastoreInstanceProvider: DatastoreInstanceProvider,
    private val filters: Filters,
    fixer: Fixer
) : EncryptableEntityLogic<Contact, ContactDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic), ContactLogic {

	companion object {
		private val logger = LoggerFactory.getLogger(ContactLogicImpl::class.java)
	}

	override suspend fun getContact(id: String) = getEntity(id)

	override fun getContacts(selectedIds: Collection<String>) = getEntities(selectedIds)

	override fun findContactsByIds(selectedIds: Collection<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.findContactsByIds(datastoreInformation, selectedIds))
	}

	override fun listContactsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<Contact> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			contactDAO.listContactsByHcPartyAndPatient(
				datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys
			)
		)
	}

	override fun listContactByHCPartyIdAndSecretPatientKey(
		hcPartyId: String,
		secretPatientKey: String,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			contactDAO.listContactsByHcPartyIdAndPatientSecretKey(
				datastoreInformation,
				hcPartyId,
				secretPatientKey,
				paginationOffset.limitIncludingKey()
			).toPaginatedFlow<Contact>(paginationOffset.limit)
		)
	}

	override fun listContactIdsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listContactIdsByHcPartyAndPatient(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys))
	}

	override suspend fun addDelegation(contactId: String, delegation: Delegation): Contact? {
		val datastoreInformation = getInstanceAndGroup()
		return getContact(contactId)?.let { c ->
			delegation.delegatedTo?.let { healthcarePartyId ->
				contactDAO.save(
					datastoreInformation, c.copy(
						delegations = c.delegations + mapOf(
							healthcarePartyId to setOf(delegation)
						)
					)
				)
			} ?: c
		}
	}

	override suspend fun addDelegations(contactId: String, delegations: List<Delegation>): Contact? {
		val datastoreInformation = getInstanceAndGroup()
		return getContact(contactId)?.let { c ->
			contactDAO.save(datastoreInformation, c.copy(delegations = c.delegations + delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } }))
		}
	}

	override suspend fun createContact(contact: Contact) = fix(contact) { fixedContact ->
		try { // Fetching the hcParty
			if(fixedContact.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
			val dataOwnerId = sessionLogic.getCurrentDataOwnerId()

			createEntities(
				setOf(
					if (fixedContact.healthcarePartyId == null) fixedContact.copy(
						healthcarePartyId = dataOwnerId,
					) else fixedContact
				)
			).firstOrNull()
		} catch (e: BulkUpdateConflictException) {
			throw UpdateConflictException("Contact already exists")
		} catch (e: Exception) {
			logger.error("createContact: " + e.message)
			throw IllegalArgumentException("Invalid contact", e)
		}
	}

	override fun createContacts(contacts: Flow<Contact>): Flow<Contact> = createEntities(contacts.map(::fix))

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun getServices(selectedServiceIds: Collection<String>): Flow<org.taktik.icure.entities.embed.Service> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val orderedIds = selectedServiceIds.toCollection(LinkedHashSet())
		val contactIds = contactDAO.listIdsByServices(datastoreInformation, orderedIds)

		val servicesToEmit = contactIds.bufferedChunksAtTransition(
			20, 100
		) { p, n -> p.serviceId == null || n.serviceId == null || p.serviceId != n.serviceId }.fold(mutableMapOf<String, org.taktik.icure.entities.embed.Service>()) { toEmit, chunkedCids ->
				val sortedCids = chunkedCids.sortedWith(compareBy({ it.serviceId }, { it.modified }, { it.contactId }))
				val filteredCidSids = sortedCids.filterIndexed { idx, cidsid -> idx == chunkedCids.size - 1 || cidsid.serviceId != sortedCids[idx + 1].serviceId }

				val contacts = contactDAO.getContacts(
					datastoreInformation, HashSet(filteredCidSids.map { it.contactId })
				)
				contacts.collect { c ->
					c.services.forEach { s ->
						val sId = s.id
						val sModified = s.modified
						if (orderedIds.contains(sId) && filteredCidSids.any { it.contactId == c.id && it.serviceId == sId }) {
							val psModified = toEmit[sId]?.modified
							if (psModified == null || sModified != null && sModified > psModified) {
								toEmit[sId] = s.pimpWithContactInformation(c)
							}
						}
					}
				}
				toEmit
			}
		orderedIds.forEach { servicesToEmit[it]?.let { s -> emit(s) } }
	}

	override fun getServicesLinkedTo(
		ids: List<String>,
		linkType: String?,
	): Flow<org.taktik.icure.entities.embed.Service> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			getServices(
				contactDAO.findServiceIdsByIdQualifiedLink(datastoreInformation, ids, linkType).toList()
			)
		)
	}

	override fun listServicesByAssociationId(associationId: String): Flow<org.taktik.icure.entities.embed.Service> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listServiceIdsByAssociationId(datastoreInformation, associationId))
	}

	override fun listServiceIdsByHcParty(hcPartyId: String) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listServiceIdsByHcParty(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId)))
	}

	override fun listServiceIdsByTag(
		hcPartyId: String,
		patientSecretForeignKeys: List<String>?,
		tagType: String,
		tagCode: String,
		startValueDate: Long?,
		endValueDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			mergeUniqueIdsForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcPartyId)) { key ->
				if (patientSecretForeignKeys == null)
					contactDAO.listServiceIdsByTag(datastoreInformation, key, tagType, tagCode, startValueDate, endValueDate, descending)
				else
					contactDAO.listServiceIdsByPatientAndTag(datastoreInformation, key, patientSecretForeignKeys, tagType, tagCode, startValueDate, endValueDate, descending)
			}
		)
	}

	override fun listServiceIdsByCode(
		hcPartyId: String,
		patientSecretForeignKeys: List<String>?,
		codeType: String,
		codeCode: String,
		startValueDate: Long?,
		endValueDate: Long?,
		descending: Boolean
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			mergeUniqueIdsForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcPartyId)) { key ->
				if (patientSecretForeignKeys == null)
					contactDAO.listServiceIdsByCode(datastoreInformation, key, codeType, codeCode, startValueDate, endValueDate, descending)
				else
					contactDAO.listServicesIdsByPatientAndCode(datastoreInformation, key, patientSecretForeignKeys, codeType, codeCode, startValueDate, endValueDate, descending)
			}
		)
	}

	override fun listContactIdsByTag(
		hcPartyId: String,
		tagType: String,
		tagCode: String,
		startValueDate: Long?,
		endValueDate: Long?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			mergeUniqueIdsForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcPartyId)) { key ->
				contactDAO.listContactIdsByTag(datastoreInformation, key, tagType, tagCode, startValueDate, endValueDate)
			}
		)
	}

	override fun listServiceIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			contactDAO.listServiceIdsByHcPartyAndIdentifiers(
				datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), identifiers
			)
		)
	}

	override fun listContactIdsByHcPartyAndIdentifiers(hcPartyId: String, identifiers: List<Identifier>): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listContactIdsByHcPartyAndIdentifiers(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), identifiers))
	}

	override fun listServicesByHcPartyAndHealthElementIds(hcPartyId: String, healthElementIds: List<String>) = flow {
		val serviceIds = listServiceIdsByHcPartyAndHealthElementIds(hcPartyId, healthElementIds)
		emitAll(getServices(serviceIds.toList()))
	}

	override fun listServiceIdsByHcPartyAndHealthElementIds(hcPartyId: String, healthElementIds: List<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listServiceIdsByHcPartyHealthElementIds(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), healthElementIds))
	}

	override fun listContactIdsByCode(
		hcPartyId: String,
		codeType: String,
		codeCode: String,
		startValueDate: Long?,
		endValueDate: Long?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			mergeUniqueIdsForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcPartyId)) { key ->
				contactDAO.listContactIdsByTag(
					datastoreInformation, key, codeType, codeCode, startValueDate, endValueDate
				)
			}
		)
	}

	override fun listContactIds(hcPartyId: String): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			mergeUniqueIdsForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcPartyId)) { key ->
				contactDAO.listContactIdsByHealthcareParty(datastoreInformation, key)
			}
		)
	}

	override fun listIdsByServices(services: Collection<String>): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listIdsByServices(datastoreInformation, services).map { it.contactId })
	}

	override fun listServicesByHcPartyAndSecretForeignKeys(
		hcPartyId: String,
		patientSecretForeignKeys: Set<String>,
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			contactDAO.listServicesIdsByPatientForeignKeys(
				datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), patientSecretForeignKeys
			)
		)
	}

	override fun listContactsByHcPartyAndFormId(hcPartyId: String, formId: String): Flow<Contact> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listContactsByHcPartyAndFormId(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), formId))
	}

	override fun listContactsByHcPartyServiceId(hcPartyId: String, serviceId: String): Flow<Contact> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listContactsByHcPartyAndServiceId(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), serviceId))
	}

	override fun listContactsByExternalId(externalId: String): Flow<Contact> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.findContactsByExternalId(datastoreInformation, externalId))
	}

	override suspend fun getServiceCodesOccurences(
		hcPartyId: String,
		codeType: String,
		minOccurences: Long,
	): List<LabelledOccurence> {
		val datastoreInformation = getInstanceAndGroup()
		val mapped = contactDAO.listCodesFrequencies(datastoreInformation, hcPartyId, codeType).filter { v -> v.second?.let { it >= minOccurences } == true }.map { v -> LabelledOccurence(v.first.components[2] as String, v.second!!) }.toList()
		return mapped.sortedByDescending { obj: LabelledOccurence -> obj.occurence }
	}

	override fun listContactsByHcPartyAndFormIds(hcPartyId: String, ids: List<String>): Flow<Contact> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listContactsByHcPartyAndFormIds(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), ids))
	}

	override fun entityWithUpdatedSecurityMetadata(entity: Contact, updatedMetadata: SecurityMetadata): Contact = entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO(): ContactDAO {
		return contactDAO
	}

	override fun filterContacts(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<Contact>) = flow {
		val ids = filters.resolve(filter.filter).toSet(TreeSet())

		val sortedIds = if (paginationOffset.startDocumentId != null) { // Sub-set starting from startDocId to the end (including last element)
			ids.dropWhile { it != paginationOffset.startDocumentId }
		} else {
			ids
		}
		val selectedIds = sortedIds.take(paginationOffset.limit + 1) // Fetching one more contacts for the start key of the next page

		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.findContactsByIds(datastoreInformation, selectedIds))
	}

	override fun filterServices(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<org.taktik.icure.entities.embed.Service>,
	) = flow {
		val ids = filters.resolve(filter.filter).toSet(LinkedHashSet())
		aggregateResults(
			ids = ids,
			limit = paginationOffset.limit,
			supplier = { serviceIds: Collection<String> ->
				filter.applyTo(
					getServices(serviceIds.toList()),
					sessionLogic.getSearchKeyMatcher()
				)
		   },
			startDocumentId = paginationOffset.startDocumentId
		).forEach { emit(it) }
	}

	override fun solveConflicts(limit: Int?): Flow<IdAndRev> = flow {
		val datastoreInformation = datastoreInstanceProvider.getInstanceAndGroup()
		emitAll(contactDAO.listConflicts(datastoreInformation).let { if (limit != null) it.take(limit) else it }.mapNotNull {
			contactDAO.get(datastoreInformation, it.id, Option.CONFLICTS)?.let { contact ->
				contact.conflicts?.mapNotNull { conflictingRevision ->
					contactDAO.get(
						datastoreInformation, contact.id, conflictingRevision
					)
				}?.fold(contact) { kept, conflict ->
					kept.merge(conflict).also {
						contactDAO.purge(datastoreInformation, conflict)
					}
				}?.let { mergedContact -> contactDAO.save(datastoreInformation, mergedContact) }
			}
		}.map { IdAndRev(it.id, it.rev) })
	}

	override fun listContactsByOpeningDate(
		hcPartyId: String,
		startOpeningDate: Long,
		endOpeningDate: Long,
		offset: PaginationOffset<ComplexKey>,
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			contactDAO.listContactsByOpeningDate(
				datastoreInformation, hcPartyId, startOpeningDate, endOpeningDate, offset.limitIncludingKey()
			).toPaginatedFlow<Contact>(offset.limit)
		)
	}
}

@ExperimentalCoroutinesApi
private fun <T> Flow<T>.bufferedChunksAtTransition(
	min: Int,
	max: Int,
	transition: (prev: T, cur: T) -> Boolean,
): Flow<List<T>> = channelFlow {
	require(min >= 2 && max >= 2 && max >= min) {
		"Min and max chunk sizes should be greater than 1, and max >= min"
	}
	val buffer = ArrayList<T>(max)
	collect {
		buffer += it
		if (buffer.size >= max) {
			var idx = buffer.size - 2
			while (idx >= 0 && !transition(buffer[idx], buffer[idx + 1])) {
				idx--
			}
			if (idx >= 0) {
				if (idx == buffer.size - 2) {
					send(buffer.subList(0, idx + 1).toList())
					val kept = buffer[buffer.size - 1]
					buffer.clear()
					buffer += kept
				} else {
					//Slow branch
					send(buffer.subList(0, idx + 1).toList())
					val kept = buffer.subList(idx + 1, buffer.size).toList()
					buffer.clear()
					buffer += kept
				}
			} else {
				//Should we throw an exception ?
				send(buffer.toList())
				buffer.clear()
			}
		} else if (min <= buffer.size && transition(buffer[buffer.size - 2], buffer[buffer.size - 1])) {
			val offered = this.trySend(buffer.subList(0, buffer.size - 1).toList()).isSuccess
			if (offered) {
				val kept = buffer[buffer.size - 1]
				buffer.clear()
				buffer += kept
			}
		}
	}
	if (buffer.size > 0) send(buffer.toList())
}.buffer(1)
