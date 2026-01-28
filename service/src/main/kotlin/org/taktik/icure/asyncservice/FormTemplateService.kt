/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.icure.entities.FormTemplate

interface FormTemplateService {
	suspend fun createFormTemplate(entity: FormTemplate): FormTemplate
	suspend fun getFormTemplate(formTemplateId: String): FormTemplate?
	suspend fun modifyFormTemplate(formTemplate: FormTemplate): FormTemplate

	// Batch methods - standard (non-conflicting with base interface)
	fun createFormTemplates(entities: Collection<FormTemplate>, createdEntities: Collection<FormTemplate>): Flow<FormTemplate>
	fun modifyFormTemplates(formTemplates: List<FormTemplate>): Flow<FormTemplate>
	fun getFormTemplates(formTemplateIds: List<String>): Flow<FormTemplate>

	/**
	 * If there is any form template with author=[userId] and guid=[formTemplateGuid] returns them, regardless of
	 * [specialityCode].
	 * Else returns all form templates with specialty.code=[specialityCode] and guid=[formTemplateGuid] regardless of
	 * [userId].
	 */
	@Deprecated("This method has unintuitive behaviour, read FormTemplateService.getFormTemplatesByGuid doc for more info")
	fun getFormTemplatesByGuid(userId: String, specialityCode: String, formTemplateGuid: String): Flow<FormTemplate>
	fun getFormTemplatesBySpecialty(specialityCode: String, loadLayout: Boolean): Flow<FormTemplate>
	fun getFormTemplatesByUser(userId: String, loadLayout: Boolean): Flow<FormTemplate>

	/**
	 * Deletes [FormTemplate]s in batch.
	 *
	 * @param ids a [Set] containing the ids of the [FormTemplate]s to delete.
	 * @return a [Flow] containing the deleted [FormTemplate]s.
	 */

	suspend fun undeleteFormTemplate(formTemplateId: String, rev: String): FormTemplate
	suspend fun deleteFormTemplate(id: String, rev: String?): DocIdentifier
	fun deleteFormTemplates(ids: List<String>): Flow<FormTemplate>
	fun deleteFormTemplatesWithRev(formTemplateIds: List<IdAndRev>): Flow<DocIdentifier>
	fun undeleteFormTemplates(formTemplateIds: List<IdAndRev>): Flow<FormTemplate>
	suspend fun purgeFormTemplate(id: String, rev: String): DocIdentifier
	fun purgeFormTemplates(formTemplateIds: List<IdAndRev>): Flow<DocIdentifier>

}
