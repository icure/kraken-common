/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.slf4j.LoggerFactory
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.ContactIdServiceId
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.data.LabelledOccurence
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.entities.pimpWithContactInformation
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.utils.aggregateResults
import org.taktik.icure.validation.aspect.Fixer
import java.util.*
import kotlin.collections.ArrayDeque

open class ContactLogicImpl(
    private val contactDAO: ContactDAO,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    private val sessionLogic: SessionInformationProvider,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    filters: Filters,
    fixer: Fixer
) : EntityWithEncryptionMetadataLogic<Contact, ContactDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters), ContactLogic {

	companion object {
		private val logger = LoggerFactory.getLogger(ContactLogicImpl::class.java)
	}

	override suspend fun getContact(id: String) = getEntity(id)

	override fun getContacts(selectedIds: Collection<String>) = getEntities(selectedIds)

	override fun findContactsByIds(selectedIds: Collection<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.findContactsByIds(datastoreInformation, selectedIds))
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listContactIdsByDataOwnerPatientOpeningDate instead")
	override fun listContactsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>): Flow<Contact> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(
				contactDAO.listContactsByHcPartyAndPatient(
					datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys
				)
			)
		}

	override fun listContactIdsByDataOwnerPatientOpeningDate(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(contactDAO.listContactIdsByDataOwnerPatientOpeningDate(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending
			)
		)
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
			contactDAO.save(
				datastoreInformation,
				c.copy(delegations = c.delegations + delegations.mapNotNull { d ->
					d.delegatedTo?.let { delegateTo ->
						delegateTo to setOf(d)
					}
				})
			)
		}
	}

	override suspend fun createContact(contact: Contact) = fix(contact, isCreate = true) { fixedContact ->
		if (fixedContact.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		createEntities(setOf(fixedContact)).firstOrNull()
	}

	override fun createContacts(contacts: Flow<Contact>): Flow<Contact> = createEntities(contacts.map { fix(it, isCreate = true) })

	private class MutablePair<A, B>(
		var first: A,
		var second: B
	)
	override fun getServices(selectedServiceIds: Collection<String>): Flow<org.taktik.icure.entities.embed.Service> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			val contactAndServiceIds = contactDAO.listLatestContactIdsByServices(datastoreInformation, selectedServiceIds.distinct()).toCollection(ArrayDeque())
			// Cache only contacts that need to be used more than once, the others can be thrown away immediately
			val expectedContactUses = contactAndServiceIds.groupingBy { it.contactId }.eachCount().filterValues { it > 1 }
			val contactsCache = mutableMapOf<String, MutablePair<Contact, Int>>()
			getContacts(contactAndServiceIds.mapTo(mutableSetOf()) { it.contactId }).collect { currContact ->
				// If the curr contact doesn't match the contact containing the next service to emit we must have that
				// contact in cache
				while (currContact.id != contactAndServiceIds.first().contactId) {
					val toEmit = contactAndServiceIds.removeFirst()
					val cached = checkNotNull(contactsCache[toEmit.contactId]) {
						"Unexpected contact ordering, next to emit was not cached"
					}
					emit(
						checkNotNull(cached.first.services.firstOrNull {
							it.id == toEmit.serviceId
						}) {
							"Contact ${toEmit.contactId} doesn't contain service ${toEmit.serviceId}"
						}.pimpWithContactInformation(cached.first)
					)
					cached.second += 1
					if (cached.second == expectedContactUses[toEmit.contactId]) {
						contactsCache.remove(toEmit.contactId)
					}
				}
				// Emit the data from current contact
				val toEmit = contactAndServiceIds.removeFirst()
				emit(
					checkNotNull(currContact.services.firstOrNull {
						it.id == toEmit.serviceId
					}) {
						"Contact ${toEmit.contactId} doesn't contain service ${toEmit.serviceId}"
					}.pimpWithContactInformation(currContact)
				)
				// If the current contact needs to be reused later we cache it.
				if (expectedContactUses.containsKey(currContact.id)) {
					contactsCache[currContact.id] = MutablePair(currContact, 1)
				}
			}

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

	override fun listServicesByAssociationId(associationId: String): Flow<org.taktik.icure.entities.embed.Service> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(contactDAO.listServicesByAssociationId(datastoreInformation, associationId))
		}

	override fun listServicesByHcPartyAndHealthElementIds(hcPartyId: String, healthElementIds: List<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val serviceIds = contactDAO.listServiceIdsByHcPartyHealthElementIds(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				healthElementIds
			)
		emitAll(getServices(serviceIds.toList()))
	}

	override fun listContactsByHcPartyAndFormId(hcPartyId: String, formId: String): Flow<Contact> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			contactDAO.listContactsByHcPartyAndFormId(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				formId
			)
		)
	}

	override fun listContactsByHcPartyServiceId(hcPartyId: String, serviceId: String): Flow<Contact> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			contactDAO.listContactsByHcPartyAndServiceId(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				serviceId
			)
		)
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
		require(getAllSearchKeysIfCurrentDataOwner(hcPartyId).size <= 1) {
			"This method is not supported for anonymous data owners"
		}
		val datastoreInformation = getInstanceAndGroup()
		val mapped = contactDAO.listCodesFrequencies(datastoreInformation, hcPartyId, codeType)
			.filter { v -> v.second?.let { it >= minOccurences } == true }
			.map { v -> LabelledOccurence(v.first.components[2] as String, v.second!!) }.toList()
		return mapped.sortedByDescending { obj: LabelledOccurence -> obj.occurence }
	}

	override fun listContactsByHcPartyAndFormIds(hcPartyId: String, ids: List<String>): Flow<Contact> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			contactDAO.listContactsByHcPartyAndFormIds(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				ids
			)
		)
	}

	override fun entityWithUpdatedSecurityMetadata(entity: Contact, updatedMetadata: SecurityMetadata): Contact =
		entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO(): ContactDAO {
		return contactDAO
	}

	override fun filterContacts(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<Contact>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter, datastoreInformation).toSet(TreeSet())

		val sortedIds =
			if (paginationOffset.startDocumentId != null) { // Sub-set starting from startDocId to the end (including last element)
				ids.dropWhile { it != paginationOffset.startDocumentId }
			} else {
				ids
			}
		val selectedIds =
			sortedIds.take(paginationOffset.limit + 1) // Fetching one more contacts for the start key of the next page

		emitAll(contactDAO.findContactsByIds(datastoreInformation, selectedIds))
	}

	override fun filterServices(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<org.taktik.icure.entities.embed.Service>,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter, datastoreInformation).toSet(LinkedHashSet())
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
		val flow = ids?.asFlow()?.mapNotNull { contactDAO.get(datastoreInformation, it, Option.CONFLICTS) }
			?: contactDAO.listConflicts(datastoreInformation)
				.mapNotNull { contactDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { contact ->
				contact.conflicts?.mapNotNull { conflictingRevision ->
					contactDAO.get(
						datastoreInformation, contact.id, conflictingRevision
					)
				}?.fold(contact to emptyList<Contact>()) { (kept, toBePurged), conflict ->
					kept.merge(conflict) to toBePurged + conflict
				}?.let { (mergedContact, toBePurged) ->
					contactDAO.save(datastoreInformation, mergedContact).also {
						toBePurged.forEach {
							if (it.rev != null && it.rev != mergedContact.rev) {
								contactDAO.purge(datastoreInformation, listOf(it)).single()
							}
						}
					}
				}
			}
			.collect { emit(IdAndRev(it.id, it.rev)) }
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
