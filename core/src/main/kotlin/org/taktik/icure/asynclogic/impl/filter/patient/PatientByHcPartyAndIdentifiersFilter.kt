package org.taktik.icure.asynclogic.impl.filter.patient

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.patient.PatientByHcPartyAndIdentifiersFilter
import org.taktik.icure.entities.Patient
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class PatientByHcPartyAndIdentifiersFilter(
	private val patientDAO: PatientDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Patient, PatientByHcPartyAndIdentifiersFilter> {
	override fun resolve(
		filter: PatientByHcPartyAndIdentifiersFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			emitAll(
				patientDAO.listPatientIdsByHcPartyAndIdentifiers(
					datastoreInformation = datastoreInformation,
					searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(requireNotNull(filter.healthcarePartyId ?: sessionLogic.getCurrentDataOwnerIdOrNull()) {
						"A PatientByHcPartyAndIdentifiersFilter must either provide an explicit dataOwnerId or must be used by a data owner user"
					}),
					identifiers = filter.identifiers,
				),
			)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
