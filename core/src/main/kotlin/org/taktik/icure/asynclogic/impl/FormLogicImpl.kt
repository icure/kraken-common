/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import org.taktik.icure.asyncdao.FormDAO
import org.taktik.icure.asynclogic.ConflictResolutionLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.FormLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.mergers.generated.FormMerger
import org.taktik.icure.validation.aspect.Fixer

open class FormLogicImpl(
	private val formDAO: FormDAO,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	sessionLogic: SessionInformationProvider,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters,
	formMerger: FormMerger,
) : EntityWithEncryptionMetadataLogic<Form, FormDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters),
	ConflictResolutionLogic by ConflictResolutionLogicImpl(formDAO, formMerger, datastoreInstanceProvider),
	FormLogic {
	override suspend fun getForm(id: String) = getEntity(id)

	override fun getForms(selectedIds: Collection<String>) = getEntities(selectedIds)

	override fun listFormsByLogicalUuid(
		formUuid: String,
		descending: Boolean,
	): Flow<Form> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(formDAO.listFormsByLogicalUuid(datastoreInformation, formUuid, descending))
	}

	override fun listFormsByUniqueId(
		lid: String,
		descending: Boolean,
	): Flow<Form> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(formDAO.listFormsByUniqueId(datastoreInformation, lid, descending))
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method is inefficient for high volumes of keys, use listFormIdsByDataOwnerPatientOpeningDate instead")
	override fun listFormsByHCPartyAndPatient(
		hcPartyId: String,
		secretPatientKeys: List<String>,
		healthElementId: String?,
		planOfActionId: String?,
		formTemplateId: String?,
	): Flow<Form> = flow {
		val datastoreInformation = getInstanceAndGroup()
		val forms = formDAO.listFormsByHcPartyPatient(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys)
		val filteredForms =
			forms.filter { f ->
				@Suppress("DEPRECATION")
				(healthElementId == null || healthElementId == f.healthElementId) &&
					(planOfActionId == null || planOfActionId == f.planOfActionId) &&
					(formTemplateId == null || formTemplateId == f.formTemplateId)
			}
		emitAll(filteredForms)
	}

	override fun listFormIdsByDataOwnerPatientOpeningDate(
		dataOwnerId: String,
		secretForeignKeys: Set<String>,
		startDate: Long?,
		endDate: Long?,
		descending: Boolean,
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			formDAO.listFormIdsByDataOwnerPatientOpeningDate(
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
		formId: String,
		delegation: Delegation,
	): Form? {
		val datastoreInformation = getInstanceAndGroup()
		val form = getForm(formId)
		return delegation.delegatedTo?.let { healthcarePartyId ->
			form?.let { c ->
				formDAO.save(
					datastoreInformation,
					c.copy(
						delegations =
						c.delegations +
							mapOf(
								healthcarePartyId to setOf(delegation),
							),
					),
				)
			}
		} ?: form
	}

	override suspend fun createForm(form: Form) = fix(form, isCreate = true) { fixedForm ->
		checkValidityForCreation(fixedForm)
		createEntity(fixedForm)
	}

	override suspend fun modifyForm(form: Form) = fix(form, isCreate = false) { fixedForm ->
		checkValidityForModification(fixedForm)
		val datastoreInformation = getInstanceAndGroup()
		formDAO.save(datastoreInformation, if (fixedForm.created == null) fixedForm.copy(created = form.created) else fixedForm)
	}

	override fun listByHcPartyAndParentId(
		hcPartyId: String,
		formId: String,
	): Flow<Form> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(formDAO.listFormsByHcPartyAndParentId(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), formId))
	}

	override suspend fun addDelegations(
		formId: String,
		delegations: List<Delegation>,
	): Form? {
		val datastoreInformation = getInstanceAndGroup()
		val form = getForm(formId)
		return form?.let {
			formDAO.save(
				datastoreInformation,
				it.copy(
					delegations =
					it.delegations +
						delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } },
				),
			)
		}
	}

	override fun entityWithUpdatedSecurityMetadata(
		entity: Form,
		updatedMetadata: SecurityMetadata,
	): Form = entity.copy(securityMetadata = updatedMetadata)

	override fun getGenericDAO(): FormDAO = formDAO

}
