/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncdao

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.EntityTemplate

interface EntityTemplateDAO : GenericDAO<EntityTemplate> {
	fun listEntityTemplatesByUserIdTypeDescr(datastoreInformation: IDatastoreInformation, userId: String, type: String, searchString: String?, includeEntities: Boolean?): Flow<EntityTemplate>
	fun listEntityTemplatesByTypeDescr(datastoreInformation: IDatastoreInformation, type: String, searchString: String?, includeEntities: Boolean?): Flow<EntityTemplate>
	fun listEntityTemplatesByUserIdTypeKeyword(datastoreInformation: IDatastoreInformation, userId: String?, type: String?, keyword: String?, includeEntities: Boolean?): Flow<EntityTemplate>
	fun listEntityTemplatesByTypeAndKeyword(datastoreInformation: IDatastoreInformation, type: String?, keyword: String?, includeEntities: Boolean?): Flow<EntityTemplate>
}
