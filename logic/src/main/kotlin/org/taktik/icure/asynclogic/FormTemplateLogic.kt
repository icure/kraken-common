/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.FormTemplate

interface FormTemplateLogic : EntityPersister<FormTemplate> {
	fun createFormTemplates(entities: Collection<FormTemplate>, createdEntities: Collection<FormTemplate>): Flow<FormTemplate>

	@Deprecated("This method has unintuitive behaviour, read FormTemplateService.getFormTemplatesByGuid doc for more info")
	fun getFormTemplatesByGuid(userId: String, specialityCode: String, formTemplateGuid: String): Flow<FormTemplate>
	@Deprecated("Use matchEntitiesBy with a FormTemplateBySpecialtyFilter instead")
	fun getFormTemplatesBySpecialty(specialityCode: String, loadLayout: Boolean): Flow<FormTemplate>
	fun getFormTemplatesByUser(userId: String, loadLayout: Boolean): Flow<FormTemplate>
}
