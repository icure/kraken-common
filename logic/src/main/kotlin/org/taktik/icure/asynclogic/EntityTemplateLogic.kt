/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.EntityTemplate

interface EntityTemplateLogic : EntityPersister<EntityTemplate> {
	suspend fun createEntityTemplate(entityTemplate: EntityTemplate): EntityTemplate

	suspend fun modifyEntityTemplate(entityTemplate: EntityTemplate): EntityTemplate?

	suspend fun getEntityTemplate(id: String): EntityTemplate?
	fun getEntityTemplates(selectedIds: Collection<String>): Flow<EntityTemplate>

	fun listEntityTemplatesBy(userId: String, entityType: String, searchString: String?, includeEntities: Boolean?): Flow<EntityTemplate>
	fun listEntityTemplatesBy(entityType: String, searchString: String?, includeEntities: Boolean?): Flow<EntityTemplate>
	fun listEntityTemplatesByKeyword(userId: String, entityType: String, keyword: String?, includeEntities: Boolean?): Flow<EntityTemplate>
	fun listEntityTemplatesByKeyword(entityType: String, keyword: String?, includeEntities: Boolean?): Flow<EntityTemplate>
}
