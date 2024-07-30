/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toCollection
import org.springframework.context.annotation.Profile
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.service.ServiceByHcPartyTagCodeDateFilter
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.utils.getLoggedHealthCarePartyId
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import javax.security.auth.login.LoginException

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByHcPartyTagCodeDateFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, Service, ServiceByHcPartyTagCodeDateFilter> {

	override fun resolve(
        filter: ServiceByHcPartyTagCodeDateFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ) = flow {
		try {
			val hcPartyId = filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)
			val searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcPartyId)
			var ids: LinkedHashSet<String>? = null
			val patientSfks = filter.patientSecretForeignKey?.let { listOf(it) }
			if (filter.tagType != null && filter.tagCode != null) {
				ids = listServiceIdsByTag(
					datastoreInformation = datastoreInformation,
					searchKeys = searchKeys,
					patientSecretForeignKeys = patientSfks,
					tagType = filter.tagType!!,
					tagCode = filter.tagCode!!,
					startValueDate = filter.startValueDate,
					endValueDate = filter.endValueDate,
					descending = filter.descending
				).toCollection(LinkedHashSet())
			}
			if (filter.codeType != null && filter.codeCode != null) {
				val byCode = listServiceIdsByCode(
					datastoreInformation = datastoreInformation,
					searchKeys = searchKeys,
					patientSecretForeignKeys = patientSfks,
					codeType = filter.tagType!!,
					codeCode = filter.tagCode!!,
					startValueDate = filter.startValueDate,
					endValueDate = filter.endValueDate,
					descending = filter.descending
				).toCollection(LinkedHashSet())
				if (ids == null) {
					ids = byCode
				} else {
					ids.retainAll(byCode)
				}
			}
			emitAll(ids?.asFlow() ?: emptyFlow())
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}

	private fun listServiceIdsByCode(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		patientSecretForeignKeys: List<String>?,
		codeType: String,
		codeCode: String,
		startValueDate: Long?,
		endValueDate: Long?,
		descending: Boolean
	): Flow<String> = mergeUniqueIdsForSearchKeys(searchKeys) { key ->
				if (patientSecretForeignKeys == null)
					contactDAO.listServiceIdsByCode(
						datastoreInformation,
						key,
						codeType,
						codeCode,
						startValueDate,
						endValueDate,
						descending
					)
				else
					contactDAO.listServicesIdsByPatientAndCode(
						datastoreInformation,
						key,
						patientSecretForeignKeys,
						codeType,
						codeCode,
						startValueDate,
						endValueDate,
						descending
					)
			}

	private fun listServiceIdsByTag(
		datastoreInformation: IDatastoreInformation,
		searchKeys: Set<String>,
		patientSecretForeignKeys: List<String>?,
		tagType: String,
		tagCode: String,
		startValueDate: Long?,
		endValueDate: Long?,
		descending: Boolean,
	): Flow<String> =
		mergeUniqueIdsForSearchKeys(searchKeys) { key ->
			if (patientSecretForeignKeys == null)
				contactDAO.listServiceIdsByTag(
					datastoreInformation,
					key,
					tagType,
					tagCode,
					startValueDate,
					endValueDate,
					descending
				)
			else
				contactDAO.listServiceIdsByPatientAndTag(
					datastoreInformation,
					key,
					patientSecretForeignKeys,
					tagType,
					tagCode,
					startValueDate,
					endValueDate,
					descending
				)
		}
}
