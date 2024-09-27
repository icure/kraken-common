/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncdao.ClassificationTemplateDAO
import org.taktik.icure.asynclogic.ClassificationTemplateLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.ClassificationTemplate
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.pagination.PaginationElement
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
    filters: Filters
) : GenericLogicImpl<ClassificationTemplate, ClassificationTemplateDAO>(fixer, datastoreInstanceProvider, filters), ClassificationTemplateLogic {
	override fun getGenericDAO(): ClassificationTemplateDAO {
		return classificationTemplateDAO
	}

	override suspend fun createClassificationTemplate(classificationTemplate: ClassificationTemplate) =
		fix(classificationTemplate) { fixedClassificationTemplate ->
			try { // Fetching the hcParty
				val userId = sessionLogic.getCurrentUserId()
				val healthcarePartyId = sessionLogic.getCurrentHealthcarePartyId()
				// Setting Classification Template attributes
				createEntities(
					setOf(
						fixedClassificationTemplate.copy(
							author = userId, responsible = healthcarePartyId
						)
					)
				).firstOrNull()
			} catch (e: Exception) {
				log.error("createClassificationTemplate: " + e.message)
				throw IllegalArgumentException("Invalid Classification Template", e)
			}
		}


	override suspend fun getClassificationTemplate(classificationTemplateId: String): ClassificationTemplate? =
		getEntity(classificationTemplateId)

	override fun getClassificationTemplates(ids: Collection<String>): Flow<ClassificationTemplate> = getEntities(ids)

	override fun listClassificationTemplates(paginationOffset: PaginationOffset<String>) =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(classificationTemplateDAO
				.findClassificationTemplates(datastoreInformation, paginationOffset.limitIncludingKey())
				.toPaginatedFlow<ClassificationTemplate>(paginationOffset.limit)
			)
		}

	companion object {
		private val log = LoggerFactory.getLogger(ClassificationTemplateLogicImpl::class.java)
	}
}
