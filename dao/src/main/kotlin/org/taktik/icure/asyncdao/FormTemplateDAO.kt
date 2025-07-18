/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.FormTemplate

interface FormTemplateDAO : GenericDAO<FormTemplate>, AttachmentManagementDAO<FormTemplate> {
	fun listFormTemplatesByUserGuid(datastoreInformation: IDatastoreInformation, userId: String, guid: String?, loadLayout: Boolean): Flow<FormTemplate>

	fun listFormsByGuid(datastoreInformation: IDatastoreInformation, guid: String, loadLayout: Boolean): Flow<FormTemplate>

	fun listFormsBySpecialtyAndGuid(datastoreInformation: IDatastoreInformation, specialityCode: String, guid: String?, loadLayout: Boolean): Flow<FormTemplate>

	suspend fun createFormTemplate(datastoreInformation: IDatastoreInformation, entity: FormTemplate): FormTemplate
}
