/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.service

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.service.ServiceByHcPartyPatientCodesFilter
import org.taktik.icure.entities.embed.Service

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByHcPartyPatientCodesFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Service, ServiceByHcPartyPatientCodesFilter> {
	override fun resolve(
		filter: ServiceByHcPartyPatientCodesFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		val searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.healthcarePartyId)
		val emitted = mutableSetOf<String>()

		for ((codeType, codeCodes) in filter.codeCodes) {
			contactDAO.listServiceIdsByDataOwnerPatientCodeCodes(
				datastoreInformation = datastoreInformation,
				searchKeys = searchKeys,
				patientSecretForeignKeys = filter.patientSecretForeignKeys,
				codeType = codeType,
				codeCodes = codeCodes,
				startValueDate = filter.startValueDate,
				endValueDate = filter.endValueDate,
			).collect { serviceId ->
				if (emitted.add(serviceId)) {
					emit(serviceId)
				}
			}
		}
	}
}
