/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.FormTemplate

interface FormTemplateLogic : EntityPersister<FormTemplate, String> {
    fun createFormTemplates(entities: Collection<FormTemplate>, createdEntities: Collection<FormTemplate>): Flow<FormTemplate>

    suspend fun createFormTemplate(entity: FormTemplate): FormTemplate

    suspend fun getFormTemplate(formTemplateId: String): FormTemplate?
    fun getFormTemplatesByGuid(userId: String, specialityCode: String, formTemplateGuid: String): Flow<FormTemplate>
    fun getFormTemplatesBySpecialty(specialityCode: String, loadLayout: Boolean): Flow<FormTemplate>
    fun getFormTemplatesByUser(userId: String, loadLayout: Boolean): Flow<FormTemplate>

    suspend fun modifyFormTemplate(formTemplate: FormTemplate): FormTemplate?
}
