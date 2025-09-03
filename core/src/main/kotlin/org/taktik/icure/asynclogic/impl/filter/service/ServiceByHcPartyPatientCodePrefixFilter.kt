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
import org.taktik.icure.domain.filter.service.ServiceByHcPartyMonthCodePrefixFilter
import org.taktik.icure.domain.filter.service.ServiceByHcPartyPatientCodePrefixFilter
import org.taktik.icure.entities.embed.Service

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByHcPartyPatientCodePrefixFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Service, ServiceByHcPartyPatientCodePrefixFilter> {
	override fun resolve(
		filter: ServiceByHcPartyPatientCodePrefixFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		emitAll(contactDAO.listServiceIdsByDataOwnerPatientCodeCodePrefix(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.healthcarePartyId),
			patientSecretForeignKeys = filter.patientSecretForeignKeys,
			codeType = filter.codeType,
			codeCodePrefix = filter.codeCodePrefix,
			startValueDate = filter.startValueDate,
			endValueDate = filter.endValueDate
		))
	}
}
