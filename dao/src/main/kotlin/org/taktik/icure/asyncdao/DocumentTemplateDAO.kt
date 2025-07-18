/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.DocumentTemplate

interface DocumentTemplateDAO :
	GenericDAO<DocumentTemplate>,
	AttachmentManagementDAO<DocumentTemplate> {
	fun listDocumentTemplatesByUserGuid(datastoreInformation: IDatastoreInformation, userId: String, guid: String? = null, loadAttachment: Boolean = true): Flow<DocumentTemplate>

	fun listDocumentTemplatesBySpecialtyAndGuid(datastoreInformation: IDatastoreInformation, healthcarePartyId: String? = null, guid: String? = null, loadAttachment: Boolean = true): Flow<DocumentTemplate>

	fun listDocumentsByTypeUserGuid(datastoreInformation: IDatastoreInformation, documentTypeCode: String, userId: String? = null, guid: String? = null, loadAttachment: Boolean = true): Flow<DocumentTemplate>

	fun evictFromCache(entity: DocumentTemplate)
	suspend fun createDocumentTemplate(datastoreInformation: IDatastoreInformation, entity: DocumentTemplate): DocumentTemplate

	/**
	 * Retrieves all the [DocumentTemplate]s in a group in a format for pagination.
	 *
	 * @param datastoreInformation an instance of [IDatastoreInformation] to identify group and CouchDB instance.
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [ViewQueryResultEvent] containing the [DocumentTemplate]s.
	 */
	fun getAllDocumentTemplates(datastoreInformation: IDatastoreInformation, paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
}
