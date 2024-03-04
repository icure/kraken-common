/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asynclogic.base.EntityWithSecureDelegationsLogic
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.data.LabelledOccurence
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.InvoiceType
import org.taktik.icure.entities.embed.InvoicingCode
import org.taktik.icure.entities.embed.MediumType
import org.taktik.icure.pagination.PaginationElement

interface InvoiceLogic : EntityPersister<Invoice, String>, EntityWithSecureDelegationsLogic<Invoice> {
	suspend fun createInvoice(invoice: Invoice): Invoice?
	suspend fun deleteInvoice(invoiceId: String): DocIdentifier?

	suspend fun getInvoice(invoiceId: String): Invoice?
	fun getInvoices(ids: List<String>): Flow<Invoice>

	suspend fun modifyInvoice(invoice: Invoice): Invoice?

	suspend fun addDelegation(invoiceId: String, delegation: Delegation): Invoice?

	/**
	 * Retrieves all the [Invoice]s for a given data owner and where [Invoice.invoiceDate] is between [fromDate] and
	 * [toDate], sorted by [Invoice.invoiceDate], in a format for pagination.
	 *
	 * @param hcPartyId the id of the data owner.
	 * @param fromDate the lower bound for [Invoice.invoiceDate]. If null, all the invoices since the beginning of time
	 * will be retrieved.
	 * @param toDate the upper bound for [Invoice.invoiceDate]. If null, all the invoice until the end of time will be
	 * retrieved.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Invoice]s.
	 */
	fun findInvoicesByAuthor(hcPartyId: String, fromDate: Long?, toDate: Long?, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	fun listInvoicesByHcPartyContacts(hcParty: String, contactIds: Set<String>): Flow<Invoice>
	fun listInvoicesByHcPartyAndRecipientIds(hcParty: String, recipientIds: Set<String?>): Flow<Invoice>

	/**
	 * Retrieves all the [Invoice]s for a data owner and which [Invoice.secretForeignKeys] contains at least one of the
	 * keys in [secretPatientKeys].
	 * Note: if the current data owner id is equal to [hcParty], then also the search keys currently available for the
	 * user will be considered when retrieving the results.
	 *
	 * @param hcParty the data owner id.
	 * @param secretPatientKeys a [Set] of secret foreign keys that should be contained in [Invoice.secretForeignKeys].
	 * @return a [Flow] of [Invoice]s.
	 */
	fun listInvoicesByHcPartyAndPatientSfks(hcParty: String, secretPatientKeys: Set<String>): Flow<Invoice>

	/**
	 * Retrieves all the [Invoice]s for a data owner and which [Invoice.secretForeignKeys] contains the [secretPatientKey].
	 * Note: differently from [listInvoicesByHcPartyAndPatientSfks], this method will NOT consider the available search
	 * keys for the current user.
	 *
	 * @param hcParty the data owner id.
	 * @param secretPatientKey the secret foreign key that should be contained in [Invoice.secretForeignKeys].
	 * @param offset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Invoice]s.
	 */
	fun listInvoicesByHcPartyAndPatientSfk(hcParty: String, secretPatientKey: String, offset: PaginationOffset<ComplexKey>): Flow<PaginationElement>
	fun listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(hcParty: String, sentMediumType: MediumType, invoiceType: InvoiceType, sent: Boolean, fromDate: Long?, toDate: Long?): Flow<Invoice>

	fun listInvoicesByHcPartySendingModeStatus(hcParty: String, sendingMode: String?, status: String?, fromDate: Long?, toDate: Long?): Flow<Invoice>

	fun listInvoicesByHcPartyAndGroupId(hcParty: String, inputGroupId: String): Flow<Invoice>
	fun listInvoicesByHcPartyAndRecipientIdsUnsent(hcParty: String, recipientIds: Set<String?>): Flow<Invoice>
	fun listInvoicesByHcPartyAndPatientSksUnsent(hcParty: String, secretPatientKeys: Set<String>): Flow<Invoice>
	fun listInvoicesByServiceIds(serviceIds: Set<String>): Flow<Invoice>

	suspend fun mergeInvoices(hcParty: String, invoices: List<Invoice>, destination: Invoice?): Invoice?

	suspend fun validateInvoice(hcParty: String, invoice: Invoice?, refScheme: String, forcedValue: String?): Invoice?
	fun appendCodes(hcPartyId: String, userId: String, insuranceId: String?, secretPatientKeys: Set<String>, type: InvoiceType, sentMediumType: MediumType, invoicingCodes: List<InvoicingCode>, invoiceId: String?, invoiceGraceTime: Int?): Flow<Invoice>

	suspend fun addDelegations(invoiceId: String, delegations: List<Delegation>): Invoice?
	fun removeCodes(userId: String, secretPatientKeys: Set<String>, serviceId: String, inputTarificationIds: List<String>): Flow<Invoice>
	fun listInvoicesHcpsByStatus(status: String, from: Long?, to: Long?, hcpIds: List<String>): Flow<Invoice>

	fun solveConflicts(limit: Int? = null): Flow<IdAndRev>

	suspend fun getTarificationsCodesOccurrences(hcPartyId: String, minOccurrences: Long): List<LabelledOccurence>
	fun listInvoicesIdsByTarificationsByCode(hcPartyId: String, codeCode: String, startValueDate: Long, endValueDate: Long): Flow<String>
	fun listInvoiceIdsByTarificationsByCode(hcPartyId: String, codeCode: String?, startValueDate: Long?, endValueDate: Long?): Flow<String>

	fun filter(filter: FilterChain<Invoice>): Flow<Invoice>

	/**
	 * Returns a flow of all the [Invoice]s for the healthcare parties which user ids are passed as parameter for all
	 * the insurances id available. The elements of the flow are ordered by sentDate.
	 * If no userId is passed, all the users of the group are used instead.
	 * This method will fail if at least one of the users is not a HCP.
	 *
	 * @param userIds a [List] of user ids to search the [Invoice]s. If null, all the users in the system will be used.
	 * @return a [Flow] of all the [Invoice]s, ordered by sentDate.
	 */
	fun getInvoicesForUsersAndInsuranceIds(userIds: List<String>?): Flow<Invoice>

	/**
	 * Returns a flow of all the unsent [Invoice]s for the healthcare parties which user ids are passed as parameter for
	 * all the insurances id available. The elements of the flow are ordered by invoiceDate.
	 * If no userId is passed, all the users of the group are used instead.
	 * This method will fail if at least one of the users is not a HCP.
	 * @param userIds a [List] of user ids to search the [Invoice]s. If null, all the users in the system will be used.
	 * @return a [Flow] of all the [Invoice]s, ordered by invoiceDate.
	 */
	fun getUnsentInvoicesForUsersAndInsuranceIds(userIds: List<String>?): Flow<Invoice>
}
