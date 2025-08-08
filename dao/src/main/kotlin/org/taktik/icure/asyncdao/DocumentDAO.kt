/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.Document

interface DocumentDAO :
	GenericDAO<Document>,
	AttachmentManagementDAO<Document> {
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Document>

	@Deprecated("This method is inefficient for high volumes of keys, use listDocumentIdsByDataOwnerPatientCreated instead")
	fun listDocumentsByHcPartyAndSecretMessageKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: List<String>): Flow<Document>

	/**
	 * Retrieves the ids of all the [Document]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of [Document.secretForeignKeys].
	 * Only the ids of the Documents where [Document.created] is not null are returned and the results are sorted by
	 * [Document.created] in ascending or descending order according to the [descending] parameter.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [Document.secretForeignKeys].
	 * @param startDate a timestamp. If not null, only the ids of the Documents where [Document.created] is greater or equal than [startDate]
	 * will be returned.
	 * @param endDate a timestamp. If not null, only the ids of the Documents where [Document.created] is less or equal than [endDate]
	 * will be returned.
	 * @param descending whether to sort the results by [Document.created] ascending or descending.
	 * @return a [Flow] of Document ids.
	 */
	fun listDocumentIdsByDataOwnerPatientCreated(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	fun listDocumentsWithNoDelegations(datastoreInformation: IDatastoreInformation, limit: Int): Flow<Document>

	/**
	 * Retrieves all the [Document]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of
	 * [Document.secretForeignKeys].
	 * Only the Documents where [Document.documentType] is equal to [documentTypeCode] will be retrieved by this method.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param documentTypeCode the [Document.documentType].
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [Document.secretForeignKeys].
	 * @return a [Flow] of [Document]s.
	 */
	fun listDocumentsByDocumentTypeHcPartySecretMessageKeys(datastoreInformation: IDatastoreInformation, documentTypeCode: String, searchKeys: Set<String>, secretForeignKeys: List<String>): Flow<Document>

	/**
	 * Retrieves all the [Document.id]s given the delegation keys in [searchKeys] (that are the data owner
	 * ids for non-anonymous data owners and the access keys for the anonymous data owners) and a set of
	 * [Document.secretForeignKeys].
	 * Only the Documents where [Document.documentType] is equal to [documentTypeCode] will be retrieved by this method.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify CouchDB instance and group.
	 * @param documentTypeCode the [Document.documentType].
	 * @param searchKeys a [Set] of search keys (Data Owner Id + access keys).
	 * @param secretForeignKeys a [Set] of [Document.secretForeignKeys].
	 * @return a [Flow] of [Document.id]s.
	 */
	fun listDocumentIdsByDocumentTypeHcPartySecretMessageKeys(datastoreInformation: IDatastoreInformation, documentTypeCode: String, searchKeys: Set<String>, secretForeignKeys: List<String>): Flow<String>

	suspend fun listDocumentsByExternalUuid(datastoreInformation: IDatastoreInformation, externalUuid: String): List<Document>
}
