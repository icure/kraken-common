/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.taktik.couchdb.DocIdentifier
import org.taktik.couchdb.entity.IdAndRev
import org.taktik.couchdb.entity.Option
import org.taktik.icure.asyncdao.FormDAO
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.FormLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.Form
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.validation.aspect.Fixer

open class FormLogicImpl(
    private val formDAO: FormDAO,
    exchangeDataMapLogic: ExchangeDataMapLogic,
    sessionLogic: SessionInformationProvider,
    datastoreInstanceProvider: DatastoreInstanceProvider,
    fixer: Fixer,
    filters: Filters
) : EntityWithEncryptionMetadataLogic<Form, FormDAO>(fixer, sessionLogic, datastoreInstanceProvider, exchangeDataMapLogic, filters), FormLogic {

	override suspend fun getForm(id: String) = getEntity(id)

	override fun getForms(selectedIds: Collection<String>) = getEntities(selectedIds)

	override fun listFormsByLogicalUuid(formUuid: String, descending: Boolean): Flow<Form> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(formDAO.listFormsByLogicalUuid(datastoreInformation, formUuid, descending))
	}

	override fun listFormsByUniqueId(lid: String, descending: Boolean): Flow<Form> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(formDAO.listFormsByUniqueId(datastoreInformation, lid, descending))
	}

	@Suppress("DEPRECATION")
	@Deprecated("This method cannot include results with secure delegations, use listFormIdsByDataOwnerPatientOpeningDate instead")
	override fun listFormsByHCPartyAndPatient(hcPartyId: String, secretPatientKeys: List<String>, healthElementId: String?, planOfActionId: String?, formTemplateId: String?): Flow<Form> =
		flow {
			val datastoreInformation = getInstanceAndGroup()
			val forms = formDAO.listFormsByHcPartyPatient(datastoreInformation, getAllSearchKeysIfCurrentDataOwner(hcPartyId), secretPatientKeys)
			val filteredForms = forms.filter { f ->
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
		descending: Boolean
	): Flow<String> = flow {
		val datastoreInformation = getInstanceAndGroup()
		emitAll(
			formDAO.listFormIdsByDataOwnerPatientOpeningDate(
				datastoreInformation,
				getAllSearchKeysIfCurrentDataOwner(dataOwnerId),
				secretForeignKeys,
				startDate,
				endDate,
				descending
			)
		)
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

	override fun solveConflicts(limit: Int?, ids: List<String>?) = flow { emitAll(doSolveConflicts(
		ids,
		limit,
		getInstanceAndGroup()
	)) }

	protected fun doSolveConflicts(
		ids: List<String>?,
		limit: Int?,
		datastoreInformation: IDatastoreInformation,
	) =  flow {
		val flow = ids?.asFlow()?.mapNotNull { formDAO.get(datastoreInformation, it, Option.CONFLICTS) }
			?: formDAO.listConflicts(datastoreInformation)
				.mapNotNull { formDAO.get(datastoreInformation, it.id, Option.CONFLICTS) }
		(limit?.let { flow.take(it) } ?: flow)
			.mapNotNull { form ->
				form.conflicts?.mapNotNull { conflictingRevision ->
					formDAO.get(
						datastoreInformation, form.id, conflictingRevision
					)
				}?.fold(form to emptyList<Form>()) { (kept, toBePurged), conflict ->
					kept.merge(conflict) to toBePurged + conflict
				}?.let { (mergedForm, toBePurged) ->
					formDAO.save(datastoreInformation, mergedForm).also {
						toBePurged.forEach {
							if (it.rev != null && it.rev != mergedForm.rev) {
								formDAO.purge(datastoreInformation, listOf(it)).single()
							}
						}
					}
				}
			}
			.collect { emit(IdAndRev(it.id, it.rev)) }
	}

	companion object {
		private val logger = LoggerFactory.getLogger(FormLogicImpl::class.java)
	}
}
