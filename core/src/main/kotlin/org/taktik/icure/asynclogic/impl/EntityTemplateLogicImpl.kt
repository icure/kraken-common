/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.EntityTemplateDAO
import org.taktik.icure.asynclogic.EntityTemplateLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.EntityTemplate
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class EntityTemplateLogicImpl(
	private val entityTemplateDAO: EntityTemplateDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : GenericLogicImpl<EntityTemplate, EntityTemplateDAO>(fixer, datastoreInstanceProvider, filters),
	EntityTemplateLogic {
	override suspend fun createEntityTemplate(entityTemplate: EntityTemplate) = fix(entityTemplate, isCreate = true) { fixedEntityTemplate ->
		val createdEntityTemplates =
			try {
				createEntities(setOf(fixedEntityTemplate))
			} catch (e: Exception) {
				throw IllegalArgumentException("Invalid template", e)
			}
		createdEntityTemplates.firstOrNull()
	}

	override suspend fun modifyEntityTemplate(entityTemplate: EntityTemplate) = fix(entityTemplate, isCreate = false) { fixedEntityTemplate ->
		val entityTemplates = setOf(fixedEntityTemplate)
		try {
			modifyEntities(entityTemplates).firstOrNull()
		} catch (e: Exception) {
			throw IllegalArgumentException("Invalid template", e)
		}
	}

	override suspend fun getEntityTemplate(id: String): EntityTemplate? = getEntity(id)

	override fun getEntityTemplates(selectedIds: Collection<String>): Flow<EntityTemplate> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(entityTemplateDAO.getEntities(datastoreInformation, selectedIds))
	}

	override fun listEntityTemplatesBy(
		userId: String,
		entityType: String,
		searchString: String?,
		includeEntities: Boolean?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(entityTemplateDAO.listEntityTemplatesByUserIdTypeDescr(datastoreInformation, userId, entityType, searchString, includeEntities))
	}

	override fun listEntityTemplatesBy(
		entityType: String,
		searchString: String?,
		includeEntities: Boolean?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(entityTemplateDAO.listEntityTemplatesByTypeDescr(datastoreInformation, entityType, searchString, includeEntities))
	}

	override fun listEntityTemplatesByKeyword(
		userId: String,
		entityType: String,
		keyword: String?,
		includeEntities: Boolean?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(entityTemplateDAO.listEntityTemplatesByUserIdTypeKeyword(datastoreInformation, userId, entityType, keyword, includeEntities))
	}

	override fun listEntityTemplatesByKeyword(
		entityType: String,
		keyword: String?,
		includeEntities: Boolean?,
	) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(entityTemplateDAO.listEntityTemplatesByTypeAndKeyword(datastoreInformation, entityType, keyword, includeEntities))
	}

	override fun getGenericDAO() = entityTemplateDAO
}
