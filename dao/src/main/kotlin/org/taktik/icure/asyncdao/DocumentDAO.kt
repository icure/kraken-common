/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.entities.Document

interface DocumentDAO : GenericDAO<Document>, AttachmentManagementDAO<Document> {
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Document>

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
	fun findDocumentIdsByDataOwnerPatientCreated(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: Set<String>, startDate: Long?, endDate: Long?, descending: Boolean): Flow<String>

	fun listDocumentsWithNoDelegations(datastoreInformation: IDatastoreInformation, limit: Int): Flow<Document>

	fun listDocumentsByDocumentTypeHcPartySecretMessageKeys(datastoreInformation: IDatastoreInformation, documentTypeCode: String, searchKeys: Set<String>, secretForeignKeys: List<String>): Flow<Document>

	suspend fun listDocumentsByExternalUuid(datastoreInformation: IDatastoreInformation, externalUuid: String): List<Document>
}
