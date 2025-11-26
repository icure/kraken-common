/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.taktik.icure.asyncdao.ClassificationDAO
import org.taktik.icure.asynclogic.ClassificationLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.Classification
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.validation.aspect.Fixer
import java.lang.UnsupportedOperationException

open class ClassificationLogicImpl(
	private val classificationDAO: ClassificationDAO,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	private val sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
) : EntityWithEncryptionMetadataLogic<Classification, ClassificationDAO>(
	fixer,
	sessionLogic,
	datastoreInstanceProvider,
	exchangeDataMapLogic,
	filters,
),
	ClassificationLogic {
	override fun entityWithUpdatedSecurityMetadata(
		entity: Classification,
		updatedMetadata: SecurityMetadata,
	): Classification = entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO(): ClassificationDAO = classificationDAO

	override suspend fun createClassification(classification: Classification) = fix(classification, isCreate = true) { fixedClassification ->
		try {
			if (fixedClassification.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
			// TODO should be covered by autofix, unless we want to enforce the matching of author/responsible with the current user
			if (sessionLogic.requestsAutofixAnonymity()) throw UnsupportedOperationException("Creating Classifications is not supported for users requesting anonymity")
			val userId = sessionLogic.getCurrentUserId()
			val healthcarePartyId = sessionLogic.getCurrentSessionContext().getHealthcarePartyId()
			createEntities(
				setOf(
					fixedClassification.copy(
						author = userId,
						responsible = healthcarePartyId,
					),
				),
			).firstOrNull()
		} catch (e: Exception) {
			log.error("createClassification: " + e.message)
			throw IllegalArgumentException("Invalid Classification", e)
		}
	}

	override suspend fun getClassification(classificationId: String) = getEntity(classificationId)

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listClassificationIdsByDataOwnerPatientCreated instead")
	override fun listClassificationsByHCPartyAndSecretPatientKeys(
		hcPartyId: String,
		secretPatientKeys: List<String>,
	): Flow<Classification> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			classificationDAO.listClassificationsByHCPartyAndSecretPatientKeys(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				secretPatientKeys,
			),
		)
	}

	override fun listClassificationIdsByDataOwnerPatientCreated(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			classificationDAO.listClassificationIdsByDataOwnerPatientCreated(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending,
			),
		)
	}

	override suspend fun addDelegation(
		classification: Classification,
		healthcarePartyId: String,
		delegation: Delegation,
	): Classification? {
		val datastoreInformation = getInstanceAndGroup()
		return classificationDAO.save(
			datastoreInformation,
			classification.copy(
				delegations =
				classification.delegations +
					mapOf(
						healthcarePartyId to setOf(delegation),
					),
			),
		)
	}

	override suspend fun addDelegations(
		classification: Classification,
		delegations: List<Delegation>,
	): Classification? {
		val datastoreInformation = getInstanceAndGroup()
		return classificationDAO.save(
			datastoreInformation,
			classification.copy(
				delegations =
				classification.delegations +
					delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } },
			),
		)
	}

	override fun getClassifications(ids: List<String>) = getEntities(ids)

	companion object {
		private val log = LoggerFactory.getLogger(ClassificationLogicImpl::class.java)
	}
}
