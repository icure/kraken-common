/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEmpty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.FormTemplateDAO
import org.taktik.icure.asynclogic.FormTemplateLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class FormTemplateLogicImpl(
	private val formTemplateDAO: FormTemplateDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters
) : GenericLogicImpl<FormTemplate, FormTemplateDAO>(fixer, datastoreInstanceProvider, filters), FormTemplateLogic {

	override fun createFormTemplates(entities: Collection<FormTemplate>, createdEntities: Collection<FormTemplate>) = flow {
		emitAll(super.createEntities(entities))
	}

	override suspend fun createFormTemplate(entity: FormTemplate) = fix(entity, isCreate = true) { fixedEntity ->
		val datastoreInformation = getInstanceAndGroup()
		formTemplateDAO.createFormTemplate(datastoreInformation, fixedEntity)
	}

	override suspend fun getFormTemplate(formTemplateId: String): FormTemplate? {
		val datastoreInformation = getInstanceAndGroup()
		return formTemplateDAO.get(datastoreInformation, formTemplateId)
	}

	@Deprecated("This method has unintuitive behaviour, read FormTemplateService.getFormTemplatesByGuid doc for more info")
	override fun getFormTemplatesByGuid(userId: String, specialityCode: String, formTemplateGuid: String): Flow<FormTemplate> = flow {
		val datastoreInformation = getInstanceAndGroup()
		formTemplateDAO.listFormTemplatesByUserGuid(datastoreInformation, userId, formTemplateGuid, true).onEmpty {
			emitAll(formTemplateDAO.listFormsBySpecialtyAndGuid(datastoreInformation, specialityCode, formTemplateGuid, true))
		}.collect(this)
	}

	override fun getFormTemplatesBySpecialty(specialityCode: String, loadLayout: Boolean): Flow<FormTemplate> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(formTemplateDAO.listFormsBySpecialtyAndGuid(datastoreInformation, specialityCode, null, loadLayout))
	}

	override fun getFormTemplatesByUser(userId: String, loadLayout: Boolean): Flow<FormTemplate> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(formTemplateDAO.listFormTemplatesByUserGuid(datastoreInformation, userId, null, loadLayout))
	}

	override suspend fun modifyFormTemplate(formTemplate: FormTemplate) = fix(formTemplate, isCreate = false) { fixedTemplate ->
		val datastoreInformation = getInstanceAndGroup()
		formTemplateDAO.save(datastoreInformation, fixedTemplate)
	}

	override fun getGenericDAO(): FormTemplateDAO {
		return formTemplateDAO
	}
}
