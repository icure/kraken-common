/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.DocumentTemplate
import org.taktik.icure.pagination.PaginationElement

interface DocumentTemplateService {
	suspend fun createDocumentTemplate(documentTemplate: DocumentTemplate): DocumentTemplate

	suspend fun getDocumentTemplate(documentTemplateId: String): DocumentTemplate?
	fun getDocumentTemplatesBySpecialty(specialityCode: String, loadAttachment: Boolean = true): Flow<DocumentTemplate>
	fun getDocumentTemplatesByDocumentType(documentTypeCode: String, loadAttachment: Boolean = true): Flow<DocumentTemplate>
	fun getDocumentTemplatesByDocumentTypeAndUser(documentTypeCode: String, userId: String, loadAttachment: Boolean = true): Flow<DocumentTemplate>
	fun getDocumentTemplatesByUser(userId: String, loadAttachment: Boolean = true): Flow<DocumentTemplate>

	suspend fun modifyDocumentTemplate(documentTemplate: DocumentTemplate): DocumentTemplate?

    /**
     * Marks a batch of entities as deleted.
     * The data of the entities is preserved, but they won't appear in most queries.
     * Ignores entities that:
     * - don't exist
     * - the user can't delete due to limited lack of write access
     * - don't match the provided revision (if provided)
     *
     * @param ids a [List] containing the ids and optionally the revisions of the entities to delete.
     * @return a [Flow] containing the deleted [DocumentTemplate]s.
     */
    fun deleteDocumentTemplates(ids: List<IdAndRev>): Flow<DocumentTemplate>
//
//    /**
//     * Marks an entity as deleted.
//     * The data of the entity is preserved, but the entity won't appear in most queries.
//     *
//     * @param id the id of the entity to delete.
//     * @param rev
//     * @return the updated [DocIdentifier] for the entity.
//     * @throws AccessDeniedException if the current user doesn't have the permission to delete the entity.
//     * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
//     * @throws ConflictRequestException if the entity rev doesn't match.
//     */
//    suspend fun deleteDocumentTemplate(id: String, rev: String?): DocIdentifier
//
//    /**
//     * Deletes an entity.
//     * An entity deleted this way can't be restored.
//     * To delete an entity this way, the user needs purge permission in addition to write access to the entity.
//     *
//     * @param id the id of the entity
//     * @param rev the latest known revision of the entity.
//     * @throws AccessDeniedException if the current user doesn't have the permission to purge the entity.
//     * @throws NotFoundRequestException if the entity with the specified [id] does not exist.
//     * @throws ConflictRequestException if the entity rev doesn't match.
//     */
//    suspend fun purgeDocumentTemplate(id: String, rev: String): DocIdentifier
//
//    /**
//     * Restores an entity marked as deleted.
//     * The user needs to have write access to the entity
//     * @param id the id of the entity marked to restore
//     * @param rev the revision of the entity after it was marked as deleted
//     * @return the restored entity
//     */
//    suspend fun undeleteDocumentTemplate(id: String, rev: String): DocumentTemplate

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
