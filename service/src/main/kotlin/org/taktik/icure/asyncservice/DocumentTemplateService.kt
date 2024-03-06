/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.DocumentTemplate
import org.taktik.icure.pagination.PaginationElement

interface DocumentTemplateService {
	suspend fun createDocumentTemplate(documentTemplate: DocumentTemplate): DocumentTemplate

	suspend fun getDocumentTemplate(documentTemplateId: String): DocumentTemplate?
	fun getDocumentTemplatesBySpecialty(specialityCode: String): Flow<DocumentTemplate>
	fun getDocumentTemplatesByDocumentType(documentTypeCode: String): Flow<DocumentTemplate>
	fun getDocumentTemplatesByDocumentTypeAndUser(documentTypeCode: String, userId: String): Flow<DocumentTemplate>
	fun getDocumentTemplatesByUser(userId: String): Flow<DocumentTemplate>

	suspend fun modifyDocumentTemplate(documentTemplate: DocumentTemplate): DocumentTemplate?

	/**
	 * Creates a batch of [DocumentTemplate]s.
	 *
	 * @param entities a [Collection] of [DocumentTemplate]s to create.
	 * @return a [Flow] containing all the [DocumentTemplate]s successfully created.
	 */
	fun createDocumentTemplates(entities: Collection<DocumentTemplate>): Flow<DocumentTemplate>

	/**
	 * Deletes [DocumentTemplate]s in batch.
	 * If the user does not meet the precondition to delete [DocumentTemplate]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [Set] containing the ids of the [DocumentTemplate]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [DocumentTemplate]s that were successfully deleted.
	 */
	fun deleteDocumentTemplates(ids: Set<String>): Flow<DocIdentifier>

	/**
	 * Retrieves all the [DocumentTemplate]s in a group in a format for pagination.
	 *
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [PaginationElement] containing the [DocumentTemplate]s.
	 * @throws AccessDeniedException if the current user is not an admin or a healthcare party.
	 */
	fun getAllDocumentTemplates(paginationOffset: PaginationOffset<String>): Flow<PaginationElement>

	/**
	 * Retrieves all the [DocumentTemplate]s in a group.
	 *
	 * @return a [Flow] of [DocumentTemplate]s.
	 * @throws AccessDeniedException if the current user is not an admin or a healthcare party.
	 */
	fun getAllDocumentTemplates(): Flow<DocumentTemplate>
}
