package org.taktik.icure.asynclogic.impl.filter.patient

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.patient.PatientByDataOwnerModifiedAfterFilter
import org.taktik.icure.entities.Patient

@Service
@Profile("app")
class PatientByDataOwnerModifiedAfterFilter(
	private val patientDAO: PatientDAO,
	private val sessionInformationProvider: SessionInformationProvider,
) : Filter<String, Patient, PatientByDataOwnerModifiedAfterFilter> {
	override fun resolve(
		filter: PatientByDataOwnerModifiedAfterFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		patientDAO
			.listPatientIdsByDataOwnerModificationDate(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionInformationProvider.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
				startDate = filter.startDate,
				endDate = filter.endDate,
				descending = filter.descending ?: false,
			).also { emitAll(it) }
	}
}
