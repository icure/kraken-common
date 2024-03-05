/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.ViewRowNoDoc
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
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

	fun listInvoicesByHcPartyAndReferences(datastoreInformation: IDatastoreInformation, hcParty: String, invoiceReferences: Set<String>?): Flow<Invoice>

	fun listInvoicesByHcPartyAndReferences(datastoreInformation: IDatastoreInformation, hcParty: String, from: String?, to: String?, descending: Boolean, limit: Int): Flow<Invoice>

	fun listInvoicesByHcPartyAndGroupId(
		datastoreInformation: IDatastoreInformation,
		hcParty: String,
		inputGroupId: String
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
	fun listInvoicesByHcPartyAndPatientSfks(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretPatientKeys: Set<String>): Flow<Invoice>

	/**
	 * Retrieves all the [Invoice]s for a data owner and which [Invoice.secretForeignKeys] the [secretPatientKey] in a
	 * format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param searchKey a the data owner id.
	 * @param secretPatientKey a secret foreign key that should be contained in [Invoice.secretForeignKeys].
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [Invoice]s.
	 */
	fun listInvoicesByHcPartyAndPatientSfk(datastoreInformation: IDatastoreInformation, searchKey: String, secretPatientKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

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
