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
import org.taktik.icure.asynclogic.base.impl.EncryptableEntityLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
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
    exchangeDataMapLogic: ExchangeDataMapLogic,
    private val sessionLogic: SessionInformationProvider,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    fixer: Fixer
) : EncryptableEntityLogic<ClassificationTemplate, ClassificationTemplateDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic), ClassificationTemplateLogic {
	override fun entityWithUpdatedSecurityMetadata(
		entity: ClassificationTemplate,
		updatedMetadata: SecurityMetadata
	): ClassificationTemplate {
		return entity.copy(
			securityMetadata = updatedMetadata
		)
	}

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

	override fun deleteClassificationTemplates(ids: Set<String>) =
		flow {
			try {
				emitAll(deleteEntities(ids))
			} catch (e: Exception) {
				log.error(e.message, e)
			}
		}

	override fun deleteClassificationTemplates(ids: Flow<String>) =
		flow {
			try {
				emitAll(deleteEntities(ids))
			} catch (e: Exception) {
				log.error(e.message, e)
			}
		}

	override suspend fun addDelegation(classificationTemplate: ClassificationTemplate, healthcarePartyId: String, delegation: Delegation): ClassificationTemplate? {
		val datastoreInformation = getInstanceAndGroup()
		return classificationTemplate.let {
			classificationTemplateDAO.save(
				datastoreInformation,
				it.copy(
					delegations = it.delegations + mapOf(
						healthcarePartyId to setOf(delegation)
					)
				)
			)
		}
	}

	override suspend fun addDelegations(classificationTemplate: ClassificationTemplate, delegations: List<Delegation>): ClassificationTemplate? {
		val datastoreInformation = getInstanceAndGroup()
		return classificationTemplate.let {
			classificationTemplateDAO.save(
				datastoreInformation,
				it.copy(
					delegations = it.delegations +
						delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } }
				)
			)
		}
	}

	override fun getClassificationTemplates(ids: Collection<String>): Flow<ClassificationTemplate> = getEntities(ids)

	override fun listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<ClassificationTemplate> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(classificationTemplateDAO.listClassificationsByHCPartyAndSecretPatientKeys(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys))
		}

	override fun listClassificationsByHCPartyAndSecretPatientKey(
		hcPartyId: String,
		secretPatientKey: String,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(classificationTemplateDAO
			.listClassificationsByHCPartyAndSecretPatientKey(datastoreInformation, hcPartyId, secretPatientKey, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<ClassificationTemplate>(paginationOffset.limit)
		)
	}

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
