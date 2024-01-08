/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.springframework.security.access.AccessDeniedException
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.exceptions.NotFoundRequestException

interface ClassificationTemplateService : EntityWithSecureDelegationsService<ClassificationTemplate> {
	suspend fun createClassificationTemplate(classificationTemplate: ClassificationTemplate): ClassificationTemplate?

	suspend fun getClassificationTemplate(classificationTemplateId: String): ClassificationTemplate?

	/**
	 * Deletes [ClassificationTemplate]s in batch.
	 * If the user does not meet the precondition to delete [ClassificationTemplate]s, an error will be thrown.
	 * If the current user does not have the permission to delete one or more elements in
	 * the batch, then those elements will not be deleted and no error will be thrown.
	 *
	 * @param ids a [Collection] containing the ids of the [ClassificationTemplate]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the [ClassificationTemplate]s that were successfully deleted.
	 */
	fun deleteClassificationTemplates(ids: Collection<String>): Flow<DocIdentifier>

	/**
	 * Deletes a [ClassificationTemplate].
	 *
	 * @param classificationTemplateId the id of the [ClassificationTemplate] to delete.
	 * @return a [DocIdentifier] related to the [ClassificationTemplate] if the operation completes successfully.
	 * @throws [AccessDeniedException] if the current user does not have the permission to delete the [ClassificationTemplate].
	 * @throws [NotFoundRequestException] if an [ClassificationTemplate] with the specified [classificationTemplateId] does not exist.
	 */
	suspend fun deleteClassificationTemplate(classificationTemplateId: String): DocIdentifier

	suspend fun modifyClassificationTemplate(classificationTemplate: ClassificationTemplate): ClassificationTemplate

	suspend fun addDelegation(classificationTemplateId: String, healthcarePartyId: String, delegation: Delegation): ClassificationTemplate?

	suspend fun addDelegations(classificationTemplateId: String, delegations: List<Delegation>): ClassificationTemplate?
	fun getClassificationTemplates(ids: List<String>): Flow<ClassificationTemplate>
	fun listClasificationsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<ClassificationTemplate>

	fun listClassificationTemplates(paginationOffset: PaginationOffset<String>): Flow<ViewQueryResultEvent>
}
