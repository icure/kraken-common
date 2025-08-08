/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Invoice
import org.taktik.icure.entities.embed.InvoiceType
import org.taktik.icure.entities.embed.MediumType

interface InvoiceDAO : GenericDAO<Invoice> {

	/**
	 * Retrieves all the [Invoice]s for a given data owner and where [Invoice.invoiceDate] is between [fromDate] and
	 * [toDate], sorted by [Invoice.invoiceDate], in a format for pagination.
	 *
	 * @param datastoreInformation an instance [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param hcParty the id of the data owner.
	 * @param fromDate the lower bound for [Invoice.invoiceDate]. If null, all the invoices since the beginning of time
	 * will be retrieved.
	 * @param toDate the upper bound for [Invoice.invoiceDate]. If null, all the invoice until the end of time will be
	 * retrieved.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Invoice]s.
	 */
	fun findInvoicesByHcParty(datastoreInformation: IDatastoreInformation, hcParty: String, fromDate: Long?, toDate: Long?, paginationOffset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	fun listInvoicesByHcPartyAndContacts(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, contactId: Set<String>): Flow<Invoice>

	/**
	 * Retrieves all the [Invoice.id]s for a given data owner and where [Invoice.decisionReference] is equal to [decisionReference].
	 *
	 * @param datastoreInformation an instance [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKeys the data owner id + the access control keys.
	 * @param decisionReference the decision reference to query.
	 * @return a [Flow] of [String].
	 */
	fun listInvoiceIdsByDataOwnerDecisionReference(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, decisionReference: String): Flow<String>

	fun listInvoicesByHcPartyAndReferences(datastoreInformation: IDatastoreInformation, hcParty: String, invoiceReferences: Set<String>?): Flow<Invoice>

	fun listInvoicesByHcPartyAndReferences(datastoreInformation: IDatastoreInformation, hcParty: String, from: String?, to: String?, descending: Boolean, limit: Int): Flow<Invoice>

	fun listInvoicesByHcPartyAndGroupId(
		datastoreInformation: IDatastoreInformation,
		hcParty: String,
		inputGroupId: String,
	): Flow<Invoice>

	/**
	 * Retrieves all the [Invoice]s for a data owner using his set of [searchKeys] and where [Invoice.recipientId] is
	 * included in [recipientIds].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKeys a [Set] of search keys for the data owner.
	 * @param recipientIds a [Set] of recipient ids.
	 * @return a [Flow] of [Invoice]s.
	 */
	fun listInvoicesByHcPartyAndRecipientIds(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, recipientIds: Set<String?>): Flow<Invoice>

	/**
	 * Retrieves all the [Invoice]s for a data owner using his set of [searchKeys] and which [Invoice.secretForeignKeys]
	 * contains at least one of the keys in [secretPatientKeys].
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKeys a [Set] of search keys for the data owner.
	 * @param secretPatientKeys a [Set] of secret foreign keys that should be contained in [Invoice.secretForeignKeys].
	 * @return a [Flow] of [Invoice]s.
	 */
	@Deprecated("This method is inefficient for high volumes of keys, use listInvoiceIdsByDataOwnerPatientInvoiceDate instead")
	fun listInvoicesByHcPartyAndPatientSfks(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: Set<String>): Flow<Invoice>

	/**
	 * Retrieves the ids of all the [Invoice]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of [Invoice.secretForeignKeys].
	 * Only the ids of the Invoices where [Invoice.invoiceDate] is not null are returned and the results are sorted by
	 * [Invoice.invoiceDate] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [Invoice.secretForeignKeys].
	 * @param startDate a fuzzy date. If not null, only the ids of the Invoices where [Invoice.invoiceDate] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a fuzzy date. If not null, only the ids of the Invoices where [Invoice.invoiceDate] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [Invoice.invoiceDate] ascending or descending.
	 * @return a [Flow] of Invoice ids.
	 */
	fun listInvoiceIdsByDataOwnerPatientInvoiceDate(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	fun listInvoicesByHcPartyAndRecipientIdsUnsent(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, recipientIds: Set<String?>): Flow<Invoice>

	fun listInvoicesByHcPartyAndPatientFkUnsent(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: Set<String>): Flow<Invoice>

	fun listInvoicesByHcPartySentMediumTypeInvoiceTypeSentDate(datastoreInformation: IDatastoreInformation, hcParty: String, sentMediumType: MediumType, invoiceType: InvoiceType, sent: Boolean, fromDate: Long?, toDate: Long?): Flow<Invoice>

	fun listInvoicesByHcPartySendingModeStatus(datastoreInformation: IDatastoreInformation, hcParty: String, sendingMode: String?, status: String?, fromDate: Long?, toDate: Long?): Flow<Invoice>

	fun listInvoicesByServiceIds(datastoreInformation: IDatastoreInformation, serviceIds: Set<String>): Flow<Invoice>

	fun listInvoicesHcpsByStatus(datastoreInformation: IDatastoreInformation, status: String, from: Long?, to: Long?, hcpIds: List<String>): Flow<Invoice>

	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Invoice>

	fun listInvoiceIdsByTarificationsAndCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeCode: String?, startValueDate: Long?, endValueDate: Long?): Flow<String>

	fun listInvoiceIdsByTarificationsByCode(datastoreInformation: IDatastoreInformation, hcPartyId: String, codeCode: String?, startValueDate: Long?, endValueDate: Long?): Flow<String>
	fun listTarificationsFrequencies(datastoreInformation: IDatastoreInformation, hcPartyId: String): Flow<ViewRowNoDoc<ComplexKey, Long>>
}
