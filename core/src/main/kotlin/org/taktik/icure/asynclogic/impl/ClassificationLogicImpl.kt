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
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.ComplexKey
import org.taktik.icure.asyncdao.ClassificationDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ClassificationLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.base.impl.EncryptableEntityLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.entities.Classification
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.limitIncludingKey
import org.taktik.icure.pagination.toPaginatedFlow
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class ClassificationLogicImpl(
    private val classificationDAO: ClassificationDAO,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    private val sessionLogic: SessionInformationProvider,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    fixer: Fixer
) : EncryptableEntityLogic<Classification, ClassificationDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic), ClassificationLogic {
	override fun entityWithUpdatedSecurityMetadata(
		entity: Classification,
		updatedMetadata: SecurityMetadata
	): Classification {
		return entity.copy(securityMetadata = updatedMetadata)
	}

	override fun getGenericDAO(): ClassificationDAO {
		return classificationDAO
	}

	override suspend fun createClassification(classification: Classification) = fix(classification) { fixedClassification ->
		try { // Fetching the hcParty
			if(fixedClassification.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
			val userId = sessionLogic.getCurrentUserId()
			val healthcarePartyId = sessionLogic.getCurrentHealthcarePartyId()
			createEntities(
				setOf(
					fixedClassification.copy(
						author = userId,
						responsible = healthcarePartyId
					)
				)
			).firstOrNull()
		} catch (e: Exception) {
			log.error("createClassification: " + e.message)
			throw IllegalArgumentException("Invalid Classification", e)
		}
	}

	override suspend fun getClassification(classificationId: String) = getEntity(classificationId)

	override fun listClassificationsByHCPartyAndSecretPatientKeys(hcPartyId: String, secretPatientKeys: List<String>): Flow<Classification> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(classificationDAO.listClassificationsByHCPartyAndSecretPatientKeys(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys))
	}

	override fun listClassificationsByHCPartyAndSecretPatientKey(
		hcPartyId: String,
		secretPatientKey: String,
		paginationOffset: PaginationOffset<ComplexKey>
	): Flow<PaginationElement> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(classificationDAO
			.listClassificationsByHCPartyAndSecretPatientKey(datastoreInformation, hcPartyId, secretPatientKey, paginationOffset.limitIncludingKey())
			.toPaginatedFlow<Classification>(paginationOffset.limit)
		)
	}

	override fun deleteClassifications(ids: Collection<String>): Flow<DocIdentifier> =
		flow {
			try {
				emitAll(deleteEntities(ids.toSet().toList()))
			} catch (e: Exception) {
				log.error(e.message, e)
			}
		}

	override fun deleteClassifications(ids: Flow<String>): Flow<DocIdentifier> =
		flow {
			try {
				emitAll(deleteEntities(ids))
			} catch (e: Exception) {
				log.error(e.message, e)
			}
		}

	override suspend fun addDelegation(classification: Classification, healthcarePartyId: String, delegation: Delegation): Classification? {
		val datastoreInformation = getInstanceAndGroup()
		return  classificationDAO.save(
				datastoreInformation,
				classification.copy(
					delegations = classification.delegations + mapOf(
						healthcarePartyId to setOf(delegation)
					)
				)
			)

	}

	override suspend fun addDelegations(classification: Classification, delegations: List<Delegation>): Classification? {
		val datastoreInformation = getInstanceAndGroup()
			return classificationDAO.save(
				datastoreInformation,
				classification.copy(
					delegations = classification.delegations +
						delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } }
				)
			)
	}

	override fun getClassifications(ids: List<String>) = getEntities()

	companion object {
		private val log = LoggerFactory.getLogger(ClassificationLogicImpl::class.java)
	}
}
