/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.asyncservice.base.EntityWithConflictResolutionService
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.constants.Permissions
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.chain.FilterChain
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.InvoiceType
import org.taktik.icure.entities.embed.InvoicingCode
import org.taktik.icure.entities.embed.MediumType
import org.taktik.icure.pagination.PaginationElement

interface InvoiceService : EntityWithSecureDelegationsService<Invoice>, EntityWithConflictResolutionService {
	suspend fun createInvoice(invoice: Invoice): Invoice?
	suspend fun deleteInvoice(invoiceId: String): DocIdentifier?

	suspend fun getInvoice(invoiceId: String): Invoice?
	fun getInvoices(ids: List<String>): Flow<Invoice>

	suspend fun modifyInvoice(invoice: Invoice): Invoice?
	fun modifyInvoices(invoices: List<Invoice>): Flow<Invoice>

	suspend fun addDelegation(invoiceId: String, delegation: Delegation): Invoice?

	/**
	 * Retrieves all the [Invoice]s for a given data owner and where [Invoice.invoiceDate] is between [fromDate] and
	 * [toDate], sorted by [Invoice.invoiceDate], in a format for pagination.
	 * This method will automatically filter out all the entities that the current user is not allowed to access, but it
	 * will guarantee that the page limit specified in the [paginationOffset] is reached as long as there are entities
	 * available.
	 *
	 * @param hcPartyId the id of the data owner.
	 * @param fromDate the lower bound for [Invoice.invoiceDate]. If null, all the invoices since the beginning of time
	 * will be retrieved.
	 * @param toDate the upper bound for [Invoice.invoiceDate]. If null, all the invoice until the end of time will be
	 * retrieved.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [Invoice]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to find [Invoice]s.
	 */
	fun findInvoicesByAuthor(hcPartyId: String, fromDate: Long?, toDate: Long?, paginationOffset: PaginationOffset<ComplexKey>): Flow<PaginationElement>

	fun listInvoicesByHcPartyContacts(hcPartyId: String, contactIds: Set<String>): Flow<Invoice>
	fun listInvoicesByHcPartyAndRecipientIds(hcPartyId: String, recipientIds: Set<String?>): Flow<Invoice>

	/**
	 * Retrieves all the [Invoice]s for a data owner and which [Invoice.secretForeignKeys] contains at least one of the
	 * keys in [secretPatientKeys].
	 * Note: if the current data owner id is equal to [hcPartyId], then also the search keys currently available for the
	 * user will be considered when retrieving the results.
	 * This method will automatically filter out all the entities that the current user is not allowed to access.
	 *
	 * @param hcPartyId the data owner id.
	 * @param secretPatientKeys a [Set] of secret foreign keys that should be contained in [Invoice.secretForeignKeys].
	 * @return a [Flow] of [Invoice]s.
	 * @throws AccessDeniedException if the current user does not meet the precondition to retrieve [Invoice]s.
	 */
	fun listInvoicesByHcPartyAndPatientSfks(hcPartyId: String, secretPatientKeys: Set<String>): Flow<Invoice>

	/**
	 * Retrieves the ids of all the [Invoice]s given the [dataOwnerId] (plus all the current access keys if that is
	 * equal to the data owner id of the user making the request) and a set of [Invoice.secretForeignKeys].
	 * Only the ids of the Invoices where [Invoice.invoiceDate] is not null are returned and the results are sorted by
	 * [Invoice.invoiceDate] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param dataOwnerId the id of the data owner.
	 * @param secretForeignKeys a [Set] of [Invoice.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the Invoices where [Invoice.invoiceDate] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the Invoices where [Invoice.invoiceDate] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [Invoice.invoiceDate] ascending or descending.
	 * @return a [Flow] of Invoice ids.
	 * @throws AccessDeniedException if [dataOwnerId] is not the current data owner id and is not among the access keys
	 * and the current user does not have the permission to search Invoices for other users.
	 */
	fun listInvoiceIdsByDataOwnerPatientInvoiceDate(dataOwnerId: String, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	fun listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(hcPartyId: String, sentMediumType: MediumType, invoiceType: InvoiceType, sent: Boolean, fromDate: Long?, toDate: Long?): Flow<Invoice>
	fun listInvoicesByHcPartySendingModeStatus(hcPartyId: String, sendingMode: String?, status: String?, fromDate: Long?, toDate: Long?): Flow<Invoice>
	fun listInvoicesByHcPartyAndRecipientIdsUnsent(hcPartyId: String, recipientIds: Set<String?>): Flow<Invoice>
	fun listInvoicesByHcPartyAndPatientSksUnsent(hcPartyId: String, secretPatientKeys: Set<String>): Flow<Invoice>
	fun listInvoicesByServiceIds(serviceIds: Set<String>): Flow<Invoice>
	suspend fun mergeInvoices(hcPartyId: String, invoicesIds: List<String>, destination: Invoice?): Invoice?
	suspend fun validateInvoice(hcPartyId: String, invoice: Invoice, refScheme: String, forcedValue: String?): Invoice?
	fun appendCodes(hcPartyId: String, userId: String, insuranceId: String?, secretPatientKeys: Set<String>, type: InvoiceType, sentMediumType: MediumType, invoicingCodes: List<InvoicingCode>, invoiceId: String?, invoiceGraceTime: Int?): Flow<Invoice>

	suspend fun addDelegations(invoiceId: String, delegations: List<Delegation>): Invoice?
	fun removeCodes(userId: String, secretPatientKeys: Set<String>, serviceId: String, inputTarificationIds: List<String>): Flow<Invoice>
	fun listInvoicesHcpsByStatus(status: String, from: Long?, to: Long?, hcpIds: List<String>): Flow<Invoice>
	override fun solveConflicts(limit: Int?, ids: List<String>?): Flow<IdAndRev>
	fun listInvoicesByHcPartyAndGroupId(hcPartyId: String, inputGroupId: String): Flow<Invoice>
	suspend fun getTarificationsCodesOccurrences(hcPartyId: String, minOccurrences: Long): List<org.taktik.icure.entities.data.LabelledOccurence>
	fun listInvoicesIdsByTarificationsByCode(hcPartyId: String, codeCode: String, startValueDate: Long, endValueDate: Long): Flow<String>
	fun listInvoiceIdsByTarificationsByCode(hcPartyId: String, codeCode: String?, startValueDate: Long?, endValueDate: Long?): Flow<String>

	fun filter(filter: FilterChain<Invoice>): Flow<Invoice>

	/**
	 * Returns a flow of all the [Invoice]s for the healthcare parties which user ids are passed as parameter for all
	 * the insurances id available. The elements of the flow are ordered by sentDate.
	 * If no userId is passed, all the users of the group are used instead.
	 * This method will fail if at least one of the users is not a HCP.
	 * Only users with the [Permissions.InvoiceManagement.Maintenance.CanGetInvoicesForUsersAndInsurances] can access
	 * this method.
	 *
	 * @param userIds a [List] of user ids to search the [Invoice]s. If null, all the users in the system will be used.
	 * @return a [Flow] of all the [Invoice]s, ordered by sent date.
	 */
	fun getInvoicesForUsersAndInsuranceIds(userIds: List<String>?): Flow<Invoice>

	/**
	 * Returns a flow of all the unsent [Invoice]s for the healthcare parties which user ids are passed as parameter for
	 * all the insurances id available. The elements of the flow are ordered by invoiceDate.
	 * If no userId is passed, all the users of the group are used instead.
	 * This method will fail if at least one of the users is not a HCP.
	 * Only users with the [Permissions.InvoiceManagement.Maintenance.CanGetInvoicesForUsersAndInsurances] can access
	 * this method.
	 *
	 * @param userIds a [List] of user ids to search the [Invoice]s. If null, all the users in the system will be used.
	 * @return a [Flow] of all the [Invoice]s, ordered by invoiceDate.
	 */
	fun getUnsentInvoicesForUsersAndInsuranceIds(userIds: List<String>?): Flow<Invoice>

	/**
	 * Creates a batch of [Invoice]s. It will fail if the current user does not have the permission to create invoices.
	 * @param invoices a [Collection] of [Invoice]s to create.
	 * @return a [Flow] containing the successfully created [Invoice]s.
	 */
	fun createInvoices(invoices: Collection<Invoice>): Flow<Invoice>
}
