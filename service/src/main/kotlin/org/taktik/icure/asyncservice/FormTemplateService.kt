/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.DocIdentifier
import org.taktik.icure.entities.FormTemplate

interface FormTemplateService {
	fun createFormTemplates(entities: Collection<FormTemplate>, createdEntities: Collection<FormTemplate>): Flow<FormTemplate>
	suspend fun createFormTemplate(entity: FormTemplate): FormTemplate
	suspend fun getFormTemplate(formTemplateId: String): FormTemplate?
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
	suspend fun modifyFormTemplate(formTemplate: FormTemplate): FormTemplate?

	/**
	 * Deletes [FormTemplate]s in batch.
	 *
	 * @param ids a [Set] containing the ids of the [FormTemplate]s to delete.
	 * @return a [Flow] containing the [DocIdentifier]s of the successfully deleted [FormTemplate]s
	 */
	fun deleteFormTemplates(ids: Set<String>): Flow<DocIdentifier>
}
