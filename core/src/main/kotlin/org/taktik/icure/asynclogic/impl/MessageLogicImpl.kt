/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toSet
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.MessageDAO
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.MessageLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.MessageReadStatus
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.exceptions.CreationException
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer
import java.util.*
import javax.security.auth.login.LoginException

open class MessageLogicImpl(
    private val messageDAO: MessageDAO,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    private val sessionLogic: SessionInformationProvider,
    datastoreInstanceProvider: org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider,
    filters: Filters,
	private val userLogic: UserLogic,
    fixer: Fixer
) : EntityWithEncryptionMetadataLogic<Message, MessageDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters), MessageLogic {

	override fun listMessagesByHCPartySecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO.listMessagesByHcPartyAndPatient(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys))
	}

	override fun listMessageIdsByDataOwnerPatientSentDate(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			messageDAO.listMessageIdsByDataOwnerPatientSentDate(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending
			)
		)
	}

	override fun setStatus(messages: Collection<Message>, status: Int): Flow<Message> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			messageDAO.save(
				datastoreInformation, messages.map {
					it.copy(status = status or (it.status ?: 0))
				}.toList()
			)
		)
	}

	override fun setReadStatus(messages: Collection<Message>, userId: String?, status: Boolean, time: Long?) = flow {
		val datastoreInformation = getInstanceAndGroup()
		val timeOrNow = time ?: System.currentTimeMillis()
		val userOrSelf = userId ?: sessionLogic.getCurrentUserId()
		emitAll(
			messageDAO.save(
				datastoreInformation, messages.map { m: Message ->
					if ((m.readStatus[userId]?.time ?: 0) < timeOrNow) m.copy(
						readStatus = m.readStatus + (userOrSelf to MessageReadStatus(
							read = status, time = time
						))
					) else m
				}.toList()
			)
		)
	}

	override fun findForHcPartySortedByReceived(hcPartyId: String, paginationOffset: PaginationOffset<ComplexKey>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO
			.findMessagesByHcPartySortedByReceived(datastoreInformation, hcPartyId, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Message>(paginationOffset.limit)
		)
	}

	override fun findMessagesByFromAddress(
		partyId: String,
		fromAddress: String,
		paginationOffset: PaginationOffset<ComplexKey>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO
			.listMessagesByFromAddress(datastoreInformation, partyId, fromAddress, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Message>(paginationOffset.limit)
		)
	}

	override fun findMessagesByToAddress(partyId: String, toAddress: String, paginationOffset: PaginationOffset<ComplexKey>, reverse: Boolean) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO
			.findMessagesByToAddress(datastoreInformation, partyId, toAddress, paginationOffset.limitIncludingKey(), reverse)
			.toPaginatedFlow<Message>(paginationOffset.limit)
		)
	}

	override fun findMessagesByTransportGuidReceived(
		partyId: String,
		transportGuid: String?,
		paginationOffset: PaginationOffset<ComplexKey>
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO
			.findMessagesByTransportGuidReceived(datastoreInformation, partyId, transportGuid, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Message>(paginationOffset.limit)
		)
	}

	override fun findMessagesByTransportGuid(
		partyId: String, transportGuid: String?, paginationOffset: PaginationOffset<ComplexKey>
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO
			.findMessagesByTransportGuid(datastoreInformation, partyId, transportGuid, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Message>(paginationOffset.limit)
		)
	}

	override fun findMessagesByTransportGuidSentDate(partyId: String, transportGuid: String, fromDate: Long, toDate: Long, paginationOffset: PaginationOffset<ComplexKey>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO
			.findMessagesByTransportGuidAndSentDate(datastoreInformation, partyId, transportGuid, fromDate, toDate, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Message>(paginationOffset.limit)
		)
	}

	override suspend fun addDelegation(message: Message, delegation: Delegation): Message? {
		val datastoreInformation = getInstanceAndGroup()

		return delegation.delegatedTo?.let { healthcarePartyId ->
			message.let { c ->
				messageDAO.save(
					datastoreInformation, c.copy(
						delegations = c.delegations + mapOf(
							healthcarePartyId to setOf(delegation)
						)
					)
				)
			}
		} ?: message
	}

	override suspend fun addDelegations(message: Message, delegations: List<Delegation>): Message? {
		val datastoreInformation = getInstanceAndGroup()

		return messageDAO.save(datastoreInformation, message.copy(delegations = message.delegations + delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } }))
	}

	override fun getMessageChildren(messageId: String) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO.getChildren(datastoreInformation, messageId))
	}

	override fun getMessagesChildren(parentIds: List<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO.getMessagesChildren(datastoreInformation, parentIds))
	}

	override fun getMessagesByTransportGuids(hcpId: String, transportGuids: Set<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO.getMessagesByTransportGuids(datastoreInformation, hcpId, transportGuids))
	}

	override fun listMessagesByInvoiceIds(ids: List<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(messageDAO.listMessagesByInvoiceIds(datastoreInformation, ids.toSet()))
	}

	override fun createMessages(entities: Collection<Message>) = flow {
		val loggedUser = userLogic.getUser(sessionLogic.getCurrentUserId()) ?: throw NotFoundRequestException("Current user not found")

		emitAll(super.createEntities(entities
			.map{ fix(it) }
			.map {
			if (it.fromAddress == null || it.fromHealthcarePartyId == null) it.copy(
				fromAddress = it.fromAddress ?: loggedUser.email, fromHealthcarePartyId = it.fromHealthcarePartyId ?: loggedUser.healthcarePartyId
			)
			else it
		}))
	}

	@Throws(CreationException::class, LoginException::class)
	override suspend fun createMessage(message: Message) = fix(message) { fixedMessage ->
		if(fixedMessage.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
		createMessages(setOf(fixedMessage)).firstOrNull()
	}

	override suspend fun getMessage(messageId: String): Message? = getEntity(messageId)

	override fun getMessages(messageIds: List<String>): Flow<Message> = getEntities(messageIds)

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
		val flow = ids?.asFlow()?.mapNotNull { messageDAO.get(datastoreInformation, it, Option.CONFLICTS) }
			?: messageDAO.listConflicts(datastoreInformation)
				.mapNotNull { messageDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { message ->
				message.conflicts?.mapNotNull { conflictingRevision ->
					messageDAO.get(
						datastoreInformation, message.id, conflictingRevision
					)
				}?.fold(message to emptyList<Message>()) { (kept, toBePurged), conflict ->
					kept.merge(conflict) to toBePurged + conflict
				}?.let { (mergedMessage, toBePurged) ->
					messageDAO.save(datastoreInformation, mergedMessage).also {
						toBePurged.forEach {
							if (it.rev != null && it.rev != mergedMessage.rev) {
								messageDAO.purge(datastoreInformation, listOf(it)).single()
							}
						}
					}
				}
			}
			.collect { emit(IdAndRev(it.id, it.rev)) }
	}


	override fun filterMessages(
		paginationOffset: PaginationOffset<Nothing>,
		filter: FilterChain<Message>
	): Flow<ViewQueryResultEvent> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter, datastoreInformation).toSet(TreeSet())

		val sortedIds = if (paginationOffset.startDocumentId != null) { // Sub-set starting from startDocId to the end (including last element)
			ids.dropWhile { it != paginationOffset.startDocumentId }
		} else {
			ids
		}
		val selectedIds = sortedIds.take(paginationOffset.limit + 1) // Fetching one more messages for the start key of the next page

		emitAll(
			messageDAO.findMessagesByIds(datastoreInformation, selectedIds)
		)
	}

	override fun entityWithUpdatedSecurityMetadata(entity: Message, updatedMetadata: SecurityMetadata): Message {
		return entity.copy(securityMetadata = updatedMetadata)
	}

	override fun getGenericDAO(): MessageDAO = messageDAO
}
