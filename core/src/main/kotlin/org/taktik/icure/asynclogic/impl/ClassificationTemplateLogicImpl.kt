/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ClassificationTemplateDAO
import org.taktik.icure.asynclogic.ClassificationTemplateLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class ClassificationTemplateLogicImpl(
	private val classificationTemplateDAO: ClassificationTemplateDAO,
	private val sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : GenericLogicImpl<ClassificationTemplate, ClassificationTemplateDAO>(fixer, datastoreInstanceProvider, filters),
	ClassificationTemplateLogic {
	override fun getGenericDAO(): ClassificationTemplateDAO = classificationTemplateDAO

	override suspend fun createClassificationTemplate(classificationTemplate: ClassificationTemplate) = fix(classificationTemplate, isCreate = true) { fixedClassificationTemplate ->
		try {
			// TODO should be covered by autofix, unless we want to enforce the matching of author/responsible with the current user
			if (sessionLogic.requestsAutofixAnonymity()) throw UnsupportedOperationException("Creating Classifications is not supported for users requesting anonymity")
			val userId = sessionLogic.getCurrentUserId()
			val healthcarePartyId = sessionLogic.getCurrentSessionContext().getHealthcarePartyId()
			createEntity(
				fixedClassificationTemplate.copy(
					author = userId,
					responsible = healthcarePartyId,
				)
			)
		} catch (e: Exception) {
			log.error("createClassificationTemplate: " + e.message)
			throw IllegalArgumentException("Invalid Classification Template", e)
		}
	}

	override suspend fun getClassificationTemplate(classificationTemplateId: String): ClassificationTemplate? = getEntity(classificationTemplateId)

	override fun getClassificationTemplates(ids: Collection<String>): Flow<ClassificationTemplate> = getEntities(ids)

	override fun listClassificationTemplates(paginationOffset: PaginationOffset<String>) = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			classificationTemplateDAO
				.findClassificationTemplates(datastoreInformation, paginationOffset.limitIncludingKey())
				.toPaginatedFlow<ClassificationTemplate>(paginationOffset.limit),
		)
	}

	companion object {
		private val log = LoggerFactory.getLogger(ClassificationTemplateLogicImpl::class.java)
	}
}
