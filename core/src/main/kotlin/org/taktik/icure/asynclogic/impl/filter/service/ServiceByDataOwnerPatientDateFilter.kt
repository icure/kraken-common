package org.taktik.icure.asynclogic.impl.filter.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.service.ServiceByDataOwnerPatientDateFilter
import org.taktik.icure.entities.embed.Service
import org.springframework.stereotype.Service as SpringService

@SpringService
@Profile("app")
class ServiceByDataOwnerPatientDateFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, Service, ServiceByDataOwnerPatientDateFilter> {

	override fun resolve(
		filter: ServiceByDataOwnerPatientDateFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		contactDAO.listServiceIdsByDataOwnerPatientDate(
			datastoreInformation = datastoreInformation,
			searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
			patientSecretForeignKeys = filter.secretForeignKeys.toList(),
			startDate = filter.startDate,
			endDate = filter.endDate,
			descending = filter.descending ?: false
		).also { emitAll(it) }
	}
}