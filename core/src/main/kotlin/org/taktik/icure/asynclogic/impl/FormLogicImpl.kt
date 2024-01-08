/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.FormDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.FormLogic
import org.taktik.icure.asynclogic.base.impl.EncryptableEntityLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class FormLogicImpl(
    private val formDAO: FormDAO,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    sessionLogic: SessionInformationProvider,
    private val datastoreInstanceProvider: DatastoreInstanceProvider,
    fixer: Fixer
) : EncryptableEntityLogic<Form, FormDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic), FormLogic {

	override suspend fun getForm(id: String) = getEntity(id)

	override fun getForms(selectedIds: Collection<String>) = getEntities(selectedIds)

	override suspend fun getAllByLogicalUuid(formUuid: String): List<Form> {
		val datastoreInformation = getInstanceAndGroup()
		return formDAO.getAllByLogicalUuid(datastoreInformation, formUuid)
	}

	override suspend fun getAllByUniqueId(lid: String): List<Form> {
		val datastoreInformation = getInstanceAndGroup()
		return formDAO.getAllByUniqueId(datastoreInformation, lid)
	}

	override fun listFormsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>, healthElementId: String?, planOfActionId: String?, formTemplateId: String?): Flow<Form> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			val forms = formDAO.listFormsByHcPartyPatient(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys)
			val filteredForms = forms.filter { f ->
				(healthElementId == null || healthElementId == f.healthElementId) &&
					(planOfActionId == null || planOfActionId == f.planOfActionId) &&
					(formTemplateId == null || formTemplateId == f.formTemplateId)
			}
			emitAll(filteredForms)
		}

	override suspend fun addDelegation(formId: String, delegation: Delegation): Form? {
		val datastoreInformation = getInstanceAndGroup()
		val form = getForm(formId)
		return delegation.delegatedTo?.let { healthcarePartyId ->
			form?.let { c ->
				formDAO.save(
					datastoreInformation,
					c.copy(
						delegations = c.delegations + mapOf(
							healthcarePartyId to setOf(delegation)
						)
					)
				)
			}
		} ?: form
	}

	override suspend fun createForm(form: Form) =
		fix(form) { fixedForm ->
			if(fixedForm.rev != null) throw IllegalArgumentException("A new entity should not have a rev")
			createEntities(setOf(fixedForm)).firstOrNull()
		}

	override fun deleteForms(ids: Set<String>): Flow<DocIdentifier> =
		flow {
			val forms = getForms(ids)
			try {
				emitAll(deleteEntities(forms.map { it.id }.toList()))
			} catch (e: Exception) {
				logger.error(e.message, e)
			}
		}

	override suspend fun modifyForm(form: Form) =
		fix(form) { fixedForm ->
			val datastoreInformation = getInstanceAndGroup()
			formDAO.save(datastoreInformation, if (fixedForm.created == null) fixedForm.copy(created = form.created) else fixedForm)
		}

	override fun listByHcPartyAndParentId(hcPartyId: String, formId: String): Flow<Form> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			emitAll(formDAO.listFormsByHcPartyAndParentId(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), formId))
		}

	override suspend fun addDelegations(formId: String, delegations: List<Delegation>): Form? {
		val datastoreInformation = getInstanceAndGroup()
		val form = getForm(formId)
		return form?.let {
			formDAO.save(
				datastoreInformation,
				it.copy(
					delegations = it.delegations +
						delegations.mapNotNull { d -> d.delegatedTo?.let { delegateTo -> delegateTo to setOf(d) } }
				)
			)
		}
	}

	override fun entityWithUpdatedSecurityMetadata(entity: Form, updatedMetadata: SecurityMetadata): Form {
		return entity.copy(securityMetadata = updatedMetadata)
	}

	override fun getGenericDAO(): FormDAO {
		return formDAO
	}

	override fun solveConflicts(limit: Int?): Flow<IdAndRev> =
		flow {
			val datastoreInformation = datastoreInstanceProvider.getInstanceAndGroup()

			emitAll(
				formDAO.listConflicts(datastoreInformation).let { if (limit != null) it.take(limit) else it }.mapNotNull {
					formDAO.get(datastoreInformation, it.id, Option.CONFLICTS)?.let { form ->
						form.conflicts?.mapNotNull { conflictingRevision -> formDAO.get(datastoreInformation, form.id, conflictingRevision) }
							?.fold(form) { kept, conflict -> kept.merge(conflict).also { formDAO.purge(datastoreInformation, conflict) } }
							?.let { mergedForm -> formDAO.save(datastoreInformation, mergedForm) }
					}
				}.map { IdAndRev(it.id, it.rev) }
			)
		}

	companion object {
		private val logger = LoggerFactory.getLogger(FormLogicImpl::class.java)
	}
}
