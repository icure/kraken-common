/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.exceptions.NotFoundRequestException
import org.taktik.icure.pagination.PaginationElement

interface ClassificationTemplateService {
	suspend fun createClassificationTemplate(classificationTemplate: ClassificationTemplate): ClassificationTemplate

	suspend fun getClassificationTemplate(classificationTemplateId: String): ClassificationTemplate?

	/**
	 * Deletes [ClassificationTemplate]s in batch.
	 * If the user does not meet the precondition to delete [ClassificationTemplate]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [Collection] containing the ids of the [ClassificationTemplate]s to delete.
	 * @return a [Flow] containing the deleted [ClassificationTemplate]s.
	 */
	fun deleteClassificationTemplates(ids: Collection<String>): Flow<ClassificationTemplate>

	/**
	 * Deletes a [ClassificationTemplate].
	 *
	 * @param classificationTemplateId the id of the [ClassificationTemplate] to delete.
	 * @return the deleted [ClassificationTemplate].
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [ClassificationTemplate].
	 * @throws [NotFoundRequestException] if an [ClassificationTemplate] with the specified [classificationTemplateId] does not exist.
	 */
	suspend fun deleteClassificationTemplate(classificationTemplateId: String): ClassificationTemplate

	suspend fun modifyClassificationTemplate(classificationTemplate: ClassificationTemplate): ClassificationTemplate

	fun getClassificationTemplates(ids: List<String>): Flow<ClassificationTemplate>

	/**
	 * Retrieves all the [ClassificationTemplate]s in a group in a format for pagination.
	 * This method will filter out all the entities that the current user is not allowed to access, but it will guarantee
	 * that the page size specified in the [paginationOffset] is reached as long as there are available elements.
	 *
	 * @param paginationOffset a [PaginationOffset] of [String] for pagination.
	 * @return a [Flow] of [PaginationElement] wrapping the [ClassificationTemplate]s.
	 */
	fun listClassificationTemplates(paginationOffset: PaginationOffset<String>): Flow<PaginationElement>
}
