/*
 * Copyright (c) 2026. Taktik SA, All rights reserved.
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
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.domain.filter.service.ServiceByDataOwnerPatientCodeCodesFilter
import org.taktik.icure.entities.embed.Service

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByDataOwnerPatientCodeCodesFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Service, ServiceByDataOwnerPatientCodeCodesFilter> {
	override val entity get() = contactDAO.entityClass
	override val views = listOf("service_by_all_delegates_patient_code_prefix")

	override fun resolve(
		filter: ServiceByDataOwnerPatientCodeCodesFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		emitAll(contactDAO.listServiceIdsByDataOwnerPatientCodeCodes(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			patientSecretForeignKeys = filter.patientSecretForeignKeys,
			codeTypesAndCodes = filter.codeCodes,
			startValueDate = filter.startValueDate,
			endValueDate = filter.endValueDate,
			filterVersion = filter.filterVersion ?: VersionFiltering.ANY,
		))
	}
}
