/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Document

interface DocumentDAO : GenericDAO<Document>, AttachmentManagementDAO<Document> {
	fun listConflicts(datastoreInformation: IDatastoreInformation): Flow<Document>

	fun listDocumentsByHcPartyAndSecretMessageKeys(datastoreInformation: IDatastoreInformation, searchKeys: Set<String>, secretForeignKeys: List<String>): Flow<Document>

	/**
	 * Retrieves all the [Document]s for the given healthcare party id and secret foreign key in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param hcPartyId the healthcare party id.
	 * @param secretForeignKey the patient secret foreign key.
	 * @param paginationOffset a [PaginationOffset] of [ComplexKey] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] wrapping the [Document]s.
	 */
	fun listDocumentsByHcPartyIdAndSecretMessageKey(datastoreInformation: IDatastoreInformation, hcPartyId: String, secretForeignKey: String, paginationOffset: PaginationOffset<ComplexKey>): Flow<ViewQueryResultEvent>

	fun listDocumentsWithNoDelegations(datastoreInformation: IDatastoreInformation, limit: Int): Flow<Document>

	fun listDocumentsByDocumentTypeHcPartySecretMessageKeys(datastoreInformation: IDatastoreInformation, documentTypeCode: String, searchKeys: Set<String>, secretForeignKeys: List<String>): Flow<Document>

	suspend fun listDocumentsByExternalUuid(datastoreInformation: IDatastoreInformation, externalUuid: String): List<Document>
}
