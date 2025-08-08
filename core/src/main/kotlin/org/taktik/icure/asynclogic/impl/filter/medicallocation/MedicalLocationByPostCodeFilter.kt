package org.taktik.icure.asynclogic.impl.filter.medicallocation

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MedicalLocationDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.medicallocation.MedicalLocationByPostCodeFilter
import org.taktik.icure.entities.MedicalLocation

@Service
@Profile("app")
class MedicalLocationByPostCodeFilter(
	private val medicalLocationDAO: MedicalLocationDAO,
) : Filter<String, MedicalLocation, MedicalLocationByPostCodeFilter> {
	override fun resolve(
		filter: MedicalLocationByPostCodeFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = medicalLocationDAO.idsByPostCode(
		datastoreInformation = datastoreInformation,
		postCode = filter.postCode,
	)
}
