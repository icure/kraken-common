/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import com.google.common.base.Strings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.couchdb.id.UUIDGenerator
import org.taktik.icure.asyncdao.InvoiceDAO
import org.taktik.icure.asynclogic.EntityReferenceLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.InsuranceLogic
import org.taktik.icure.asynclogic.InvoiceLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.UserLogic
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Insurance
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.data.LabelledOccurence
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.InvoiceType
import org.taktik.icure.entities.embed.InvoicingCode
import org.taktik.icure.entities.embed.MediumType
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.mergeUniqueValuesForSearchKeys
import org.taktik.icure.validation.aspect.Fixer
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.abs
import kotlin.math.max

open class InvoiceLogicImpl (
	filters: Filters,
	private val userLogic: UserLogic,
	private val insuranceLogic: InsuranceLogic,
	private val uuidGenerator: UUIDGenerator,
	private val entityReferenceLogic: EntityReferenceLogic,
	private val invoiceDAO: InvoiceDAO,
	sessionLogic: SessionInformationProvider,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer
) : EntityWithEncryptionMetadataLogic<Invoice, InvoiceDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters), InvoiceLogic {

	override suspend fun createInvoice(invoice: Invoice) =
		fix(invoice, isCreate = true) { fixedInvoice ->
			if(fixedInvoice.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
			val datastoreInformation = getInstanceAndGroup()
			invoiceDAO.create(datastoreInformation, fixedInvoice)
		}

	override suspend fun getInvoice(invoiceId: String): Invoice? = getEntity(invoiceId)

	override fun getInvoices(ids: List<String>): Flow<Invoice> = getEntities(ids)

	override suspend fun modifyInvoice(invoice: Invoice)= modifyEntities(listOf(invoice)).firstOrNull()

	override suspend fun addDelegation(invoiceId: String, delegation: Delegation): Invoice? {
		val datastoreInformation = getInstanceAndGroup()
		val invoice = getInvoice(invoiceId)
		return delegation.delegatedTo?.let { healthcarePartyId ->
			invoice?.let { c ->
				invoiceDAO.save(
					datastoreInformation,
					c.copy(
						delegations = c.delegations + mapOf(
							healthcarePartyId to setOf(delegation)
						)
					)
				)
			}
		} ?: invoice
	}

	override suspend fun addDelegations(invoiceId: String, delegations: List<Delegation>): Invoice? {
		val datastoreInformation = getInstanceAndGroup()
		val invoice = getInvoice(invoiceId)
		return invoice?.let {
			return invoiceDAO.save(
				datastoreInformation,
				it.copy(
					delegations = it.delegations +
						delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } }
				)
			)
		}
	}

	override fun findInvoicesByAuthor(
		hcPartyId: String,
		fromDate: Long?,
		toDate: Long?,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(invoiceDAO
			.findInvoicesByHcParty(datastoreInformation, hcPartyId, fromDate, toDate, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Invoice>(paginationOffset.limit)
		)
	}


	override fun listInvoicesByHcPartyContacts(hcParty: String, contactIds: Set<String>): Flow<Invoice> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(invoiceDAO.listInvoicesByHcPartyAndContacts(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcParty), contactIds))
		}

	override fun listInvoiceIdsByDataOwnerDecisionReference(
		dataOwnerId: String,
		decisionReference: String
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(invoiceDAO.listInvoiceIdsByDataOwnerDecisionReference(
			datastoreInformation,
			getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
			decisionReference
		))
	}

	override fun listInvoicesByHcPartyAndRecipientIds(hcParty: String, recipientIds: Set<String?>): Flow<Invoice> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(invoiceDAO.listInvoicesByHcPartyAndRecipientIds(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcParty), recipientIds))
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listInvoiceIdsByDataOwnerPatientInvoiceDate instead")
	override fun listInvoicesByHcPartyAndPatientSfks(hcParty: String, secretPatientKeys: Set<String>): Flow<Invoice> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(invoiceDAO.listInvoicesByHcPartyAndPatientSfks(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcParty), secretPatientKeys))
		}

	override fun listInvoiceIdsByDataOwnerPatientInvoiceDate(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			invoiceDAO.listInvoiceIdsByDataOwnerPatientInvoiceDate(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending
			)
		)
	}

	override fun listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(hcParty: String, sentMediumType: MediumType, invoiceType: InvoiceType, sent: Boolean, fromDate: Long?, toDate: Long?): Flow<Invoice> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(
				mergeUniqueValuesForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcParty)) { key ->
					invoiceDAO.listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(
						datastoreInformation,
						key,
						sentMediumType,
						invoiceType,
						sent,
						fromDate,
						toDate
					)
				}
			)
		}

	override fun listInvoicesByHcPartySendingModeStatus(hcParty: String, sendingMode: String?, status: String?, fromDate: Long?, toDate: Long?): Flow<Invoice> =
		flow {
			val datastoreInformation = getInstanceAndGroup()

			emitAll(
				mergeUniqueValuesForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcParty)) { key ->
					invoiceDAO.listInvoicesByHcPartySendingModeStatus(
						datastoreInformation,
						key,
						sendingMode,
						status,
						fromDate,
						toDate
					)
				}
			)
		}

	override fun listInvoicesByHcPartyAndGroupId(hcParty: String, inputGroupId: String): Flow<Invoice> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(
				mergeUniqueValuesForSearchKeys(getAllSearchKeysIfCurrentDataOwner(hcParty)) { key ->
					invoiceDAO.listInvoicesByHcPartyAndGroupId(datastoreInformation, key, inputGroupId)
				}
			)
		}

	override fun listInvoicesByHcPartyAndRecipientIdsUnsent(hcParty: String, recipientIds: Set<String?>): Flow<Invoice> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(invoiceDAO.listInvoicesByHcPartyAndRecipientIdsUnsent(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcParty), recipientIds))
		}

	override fun listInvoicesByHcPartyAndPatientSksUnsent(hcParty: String, secretPatientKeys: Set<String>): Flow<Invoice> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(invoiceDAO.listInvoicesByHcPartyAndPatientFkUnsent(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcParty), secretPatientKeys))
		}


	override fun listInvoicesByServiceIds(serviceIds: Set<String>): Flow<Invoice> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(invoiceDAO.listInvoicesByServiceIds(datastoreInformation, serviceIds))
		}


	override suspend fun mergeInvoices(hcParty: String, invoices: List<Invoice>, destination: Invoice?): Invoice? {
		if (destination == null) return null
		for (i in invoices) {
			deleteEntity(i.id, i.rev)
		}
		return modifyInvoice(destination.copy(invoicingCodes = destination.invoicingCodes + invoices.flatMap { it.invoicingCodes }))
	}

	override suspend fun validateInvoice(hcParty: String, invoice: Invoice?, refScheme: String, forcedValue: String?): Invoice? {
		if (invoice == null) return null
		val datastoreInformation = getInstanceAndGroup()

		return modifyInvoice(
			invoice.copy(
				sentDate = System.currentTimeMillis(),
				invoiceReference = if (forcedValue != null || !Strings.isNullOrEmpty(invoice.invoiceReference)) {
					forcedValue
				} else {
					val ldt = invoice.invoiceDate?.let { FuzzyValues.getDateTime(it) }
						?: LocalDateTime.now(ZoneId.systemDefault())
					val f: NumberFormat = DecimalFormat("00")
					val startScheme = refScheme.replace("yyyy".toRegex(), "" + ldt.year).replace("MM".toRegex(), f.format(ldt.monthValue.toLong())).replace("dd".toRegex(), "" + f.format(ldt.dayOfMonth.toLong()))
					val endScheme = refScheme.replace("0".toRegex(), "9").replace("yyyy".toRegex(), "" + ldt.year).replace("MM".toRegex(), "" + f.format(ldt.monthValue.toLong())).replace("dd".toRegex(), "" + f.format(ldt.dayOfMonth.toLong()))
					val prefix = "invoice:" + invoice.author + ":xxx:"
					val fix = startScheme.replace("0+$".toRegex(), "")
					val reference = entityReferenceLogic.getLatest(prefix + fix)
					if (reference == null || !reference.id.startsWith(prefix)) {
						val prevInvoices = invoiceDAO.listInvoicesByHcPartyAndReferences(datastoreInformation, hcParty, endScheme, null, true, 1)
						val first = prevInvoices.firstOrNull()
						"" + if (first?.invoiceReference != null) max(java.lang.Long.valueOf(first.invoiceReference) + 1L, java.lang.Long.valueOf(startScheme) + 1L) else java.lang.Long.valueOf(startScheme) + 1L
					} else {
						fix + (reference.id.substring(prefix.length + fix.length).toInt() + 1)
					}
				}
			)
		)
	}

	override fun appendCodes(hcPartyId: String, userId: String, insuranceId: String?, secretPatientKeys: Set<String>, type: InvoiceType, sentMediumType: MediumType, invoicingCodes: List<InvoicingCode>, invoiceId: String?, invoiceGraceTime: Int?): Flow<Invoice> = flow {
		val fixedCodes = if (sentMediumType == MediumType.efact) {
			invoicingCodes.map { c -> c.copy(pending = true) }
		} else invoicingCodes
		val invoiceGraceTimeInDays = invoiceGraceTime ?: 0
		val selectedInvoice = if (invoiceId != null) getInvoice(invoiceId) else null
		var invoices = if (selectedInvoice != null) mutableListOf() else listInvoicesByHcPartyAndPatientSksUnsent(hcPartyId, secretPatientKeys)
			.filter { i -> i.invoiceType == type && i.sentMediumType == sentMediumType && if (insuranceId == null) i.recipientId == null else insuranceId == i.recipientId }.toList().toMutableList()
		if (selectedInvoice == null && invoices.isEmpty()) {
			invoices = listInvoicesByHcPartyAndRecipientIdsUnsent(
				hcPartyId,
				insuranceId?.let { setOf(it) }
					?: setOf()
			).filter { i -> i.invoiceType == type && i.sentMediumType == sentMediumType && i.secretForeignKeys == secretPatientKeys }.toList().toMutableList()
		}

		val modifiedInvoices: MutableList<Invoice> = LinkedList()
		val createdInvoices: MutableList<Invoice> = LinkedList()

		for (invoicingCode in fixedCodes) {
			val icDateTime = invoicingCode.dateCode?.let { FuzzyValues.getDateTime(it) }
			val unsentInvoice = selectedInvoice
					?: if (icDateTime != null) invoices.firstOrNull { i ->
						val invoiceDate = i.invoiceDate?.let { FuzzyValues.getDateTime(it) }
						invoiceDate != null && abs(
							invoiceDate.withHour(0).withMinute(0).withSecond(0).withNano(0)
								.until(icDateTime, ChronoUnit.DAYS)
						) <= invoiceGraceTimeInDays
					} else null

			if (unsentInvoice != null) {
				if (!createdInvoices.contains(unsentInvoice)) {
					modifyInvoice(
						unsentInvoice.copy(
							invoicingCodes = unsentInvoice.invoicingCodes + listOf(invoicingCode)
						)
					)?.let {
						modifiedInvoices.add(it)
						emit(it)
					}
				}
			} else {
				val now = System.currentTimeMillis()
				val newInvoice = Invoice(
					id = uuidGenerator.newGUID().toString(),
					invoiceDate = invoicingCode.dateCode ?: now,
					invoiceType = type,
					sentMediumType = sentMediumType,
					recipientId = insuranceId,
					recipientType = if (type == InvoiceType.mutualfund || type == InvoiceType.payingagency) Insurance::class.java.name else Patient::class.java.name,
					invoicingCodes = listOf(invoicingCode),
					author = userId,
					responsible = hcPartyId,
					created = now,
					modified = now,
					careProviderType = "persphysician",
					invoicePeriod = 0,
					thirdPartyPaymentJustification = "0"
				)

				//The invoice must be completed with ids and delegations and created on the server
				createdInvoices.add(newInvoice)
				emit(newInvoice)
				invoices.add(newInvoice)
			}
		}
	}

	override fun removeCodes(userId: String, secretPatientKeys: Set<String>, serviceId: String, inputTarificationIds: List<String>): Flow<Invoice> = flow {
		val tarificationIds = inputTarificationIds.toMutableList()
		val user = userLogic.getUser(userId, false)
		if (user != null) {
			val invoices = listInvoicesByHcPartyAndPatientSksUnsent(user.healthcarePartyId ?: throw IllegalArgumentException("The provided user must be linked to an hcp"), secretPatientKeys)
				.filter { i -> i.invoicingCodes.any { ic -> serviceId == ic.serviceId && tarificationIds.contains(ic.tarificationId) } }
				.toList().sortedWith { a: Invoice, b: Invoice ->
					(b.invoiceDate ?: 99999999999999L).compareTo(a.invoiceDate ?: 0L)
				}
			for (i in invoices) {
				var hasChanged = false
				val l: MutableList<InvoicingCode> = LinkedList(i.invoicingCodes)
				for (ic in i.invoicingCodes) {
					if (tarificationIds.contains(ic.tarificationId)) {
						l.remove(ic)
						tarificationIds.remove(ic.tarificationId)
						hasChanged = true
					}
				}
				if (hasChanged) {
					modifyInvoice(i.copy(invoicingCodes = l))?.let { emit(it) }
				}
			}
		}
	}

	override fun listInvoicesHcpsByStatus(status: String, from: Long?, to: Long?, hcpIds: List<String>): Flow<Invoice> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(invoiceDAO.listInvoicesHcpsByStatus(datastoreInformation, status, from, to, hcpIds))
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
		val flow = ids?.asFlow()?.mapNotNull { invoiceDAO.get(datastoreInformation, it, Option.CONFLICTS) }
			?: invoiceDAO.listConflicts(datastoreInformation)
				.mapNotNull { invoiceDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { invoice ->
				invoice.conflicts?.mapNotNull { conflictingRevision ->
					invoiceDAO.get(
						datastoreInformation, invoice.id, conflictingRevision
					)
				}?.fold(invoice to emptyList<Invoice>()) { (kept, toBePurged), conflict ->
					kept.merge(conflict) to toBePurged + conflict
				}?.let { (mergedInvoice, toBePurged) ->
					invoiceDAO.save(datastoreInformation, mergedInvoice).also {
						toBePurged.forEach {
							if (it.rev != null && it.rev != mergedInvoice.rev) {
								invoiceDAO.purge(datastoreInformation, listOf(it)).single()
							}
						}
					}
				}
			}
			.collect { emit(IdAndRev(it.id, it.rev)) }
	}


	override suspend fun getTarificationsCodesOccurrences(hcPartyId: String, minOccurrences: Long): List<LabelledOccurence> {
		require(getAllSearchKeysIfCurrentDataOwner(hcPartyId).size <= 1) {
			"This method is not supported for anonymous data owners"
		}
		val datastoreInformation = getInstanceAndGroup()
		return invoiceDAO.listTarificationsFrequencies(datastoreInformation, hcPartyId)
			.filter { v -> v.value != null && v.value!! >= minOccurrences  && v.key != null && v.key!!.components.size > 1 && v.key!!.components[1] != null }
			.map { v -> LabelledOccurence(v.key!!.components[1] as String, v.value!!) }
			.toList().sortedByDescending { it.occurence }
	}

	override fun listInvoicesIdsByTarificationsByCode(hcPartyId: String, codeCode: String, startValueDate: Long, endValueDate: Long): Flow<String> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(invoiceDAO.listInvoiceIdsByTarificationsAndCode(datastoreInformation, hcPartyId, codeCode, startValueDate, endValueDate))
		}

	override fun filter(filter: FilterChain<Invoice>) = flow {
		val dataStoreInformation = getInstanceAndGroup()
		val ids = filters.resolve(filter.filter, dataStoreInformation).toList()
		val invoices = getInvoices(ids)
		val predicate = filter.predicate
		emitAll(if (predicate != null) invoices.filter { predicate.apply(it) } else invoices)
	}

	override fun entityWithUpdatedSecurityMetadata(entity: Invoice, updatedMetadata: SecurityMetadata): Invoice {
		return entity.copy(securityMetadata = updatedMetadata)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun getInvoicesForUsersAndInsuranceIds(userIds: List<String>?): Flow<Invoice> = flow {
		val users = if (userIds == null) userLogic.getEntities() else userLogic.getUsers(userIds)
		val insuranceIds = insuranceLogic.getEntityIds().toList().toSet()
		users.flatMapConcat { user ->
			user.healthcarePartyId?.let { hcpId ->
				listInvoicesByHcPartyAndRecipientIds(hcpId, insuranceIds).filter { iv -> user.id == iv.author }
			} ?: throw IllegalArgumentException("Provided user is not a Healthcare Party")
		}.toList()
			.sortedWith(Comparator.comparing { iv: Invoice -> iv.sentDate ?: 0L })
			.forEach { emit(it) }
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	override fun getUnsentInvoicesForUsersAndInsuranceIds(userIds: List<String>?): Flow<Invoice> = flow {
		val users = if (userIds == null) userLogic.getEntities() else userLogic.getUsers(userIds)
		val insuranceIds = insuranceLogic.getEntityIds().toList().toSet()
		users.flatMapConcat { u ->
			u.healthcarePartyId?.let { hcpId ->
				listInvoicesByHcPartyAndRecipientIdsUnsent(hcpId, insuranceIds).filter { iv -> u.id == iv.author }
			} ?: throw IllegalArgumentException("Provided user is not a Healthcare Party")
		}.toList()
			.sortedWith(Comparator.comparing { invoice -> invoice.invoiceDate ?: 0L })
			.forEach { emit(it) }
	}


	override fun getGenericDAO(): InvoiceDAO {
		return invoiceDAO
	}
}
